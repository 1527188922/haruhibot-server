# haruhibot-server 配置管理方案

> 本文档描述 **已落地** 的配置管理实现。
> 一句话概括：**`./config/` 下的配置文件是配置的唯一真源（配置不进数据库），按文件分组，通过 `Configs` 静态快照读取、`ConfigHub` 写入与热更新通知，WebUI「系统管理 / 配置管理」页按文件分组编辑。**

---

## 1. 设计目标（对应最初的四种特征）

| 原始痛点 | 本方案的解决方式 |
|---|---|
| 改完不需要重启就能生效 | 每个配置项声明 `hot` 属性；`hot=true` 时改完**立即生效**（写文件 → 换内存快照 → 按 key 通知订阅者） |
| `application.yml` 里的配置 | `bot.*` / `job.*` 全部搬进 `./config/*.properties`；只有 `server.port` 这类必须由 Spring 直接消费的配置留在 `./config/server.yml` |
| `config` 目录下的 properties | 从"第二套体系"升级为**唯一真源**，并按业务拆成多个文件分组 |
| 静态方法中也要能用 | `Configs` 在**类加载时**就加载好不可变快照，不依赖 Spring，静态块 / 静态方法 / 构造器中调用都安全，且**永不返回 null** |
| 需要 WebUI 方便编辑 | 「系统管理 / 配置管理」页：按文件分组、类型化控件、key 级刷新、文件级整体刷新 |

---

## 2. 总体结构

```
                    ┌──────────────────────────────────────────────┐
   程序启动         │  ./config/                                   │
   （不依赖Spring） │    webui.properties    server.yml            │
        │           │    job.properties      bot.properties        │
        │           │    websocket.properties                      │
        │           │    searchimg.properties                      │
        │           │    bilibili.properties ai.properties         │
        │           │    jm.properties       url.properties        │
        │           │    db.properties                             │
        │           └───────────────┬──────────────────────────────┘
        │                           │ 启动时 / 变更时读取
        ▼                           ▼
  Configs（静态快照，不可变 Map，volatile 整体替换）  ◄──── ConfigHub（写文件 + 刷新快照 + 通知）
        │                                                      ▲
        │ 静态读取（任意时机、无锁、永不null）                    │ WebUI / 定时监听外部改动
        ▼                                                      │
  业务代码（handler / service / job / condition …）      ConfigApplier 订阅者
                                                          （OpenAiServiceHolder、JobManage …）
        ▲
        │ ConfigsEnvironmentInitializer 把同一份文件注入 Spring Environment
        └── @ConditionalOnProperty / @Value 读到的是同一个值
```

### 2.1 配置声明：`ConfigKey`

`src/main/java/com/haruhi/botServer/config/config/ConfigKey.java` 是**唯一声明处**：

| 字段 | 说明 |
|---|---|
| `file` | 所属 `ConfigFile`（决定落在哪个文件、前端归到哪个大类） |
| `key` | 配置key，同时作为 Spring 属性名 |
| `type` | `STRING / INT / BOOL / LIST / JSON / SECRET`，决定前端控件与保存校验 |
| `defaultValue` | 缺省值。配置缺失时一律回落到它 |
| `hot` | **是否可热更新**。false 表示该项在启动阶段就已确定（端口、druid开关、WebUI登录密码等） |
| `remark` | 说明，前端展示 |
| `sort` | 前端排序 |
| `legacyKeys` | 老版本用过的key，迁移时用于把旧值带过来（见 §4） |

`ConfigKey.of(key)` 按新key反查，`ConfigKey.ofLegacy(oldKey)` 按老key反查，`ConfigKey.of(file)` 取某个文件下的全部配置项。

### 2.2 文件分组：`ConfigFile`

支持两种格式：**properties（业务配置，可页面写入）** 与 **yml（需 Spring 直接消费，页面只读）**。

| 文件 | 中文名 | 内容 |
|---|---|---|
| `webui.properties` | WebUI | 登录账号密码、JWT、会话、druid监控台开关（**重启生效**） |
| `server.yml` | 服务端(yml) | `server.port`（**重启生效，页面只读**） |
| `job.properties` | 定时任务 | 各任务 `enable` / `cron`（**开关与cron均可即时生效**） |
| `bot.properties` | 机器人 | 同机部署、对外地址、超级管理员、可访问群、上传并发、`bot.switch.*` 功能开关 |
| `websocket.properties` | WebSocket | `bot.ws.access_token`、`bot.ws.max_connections` |
| `searchimg.properties` | 识图 | `searchimg.saucenao.baseurl`、`searchimg.saucenao.apikey`、`searchimg.agefans.url` |
| `bilibili.properties` | B站 | cookie、上传/下载时长限制 |
| `ai.properties` | AI | 千问key、DeepSeek key/baseurl/timeout |
| `jm.properties` | JM漫画 | 解压密码、线程数、并发、名称长度、API域名、搜索结果 |
| `url.properties` | 站点地址 | bt搜索、bt影视 |
| `db.properties` | 数据库 | 聊天记录压缩存储（**不含 `db.sql_cache`**，见 §2.7） |

### 2.3 读值入口：`Configs`

复刻了 `PropertiesUtil` 的"静态可读"机制，底层换成不可变快照：

```java
// 任意时机、静态方法内都可用，不会 NPE、不会返回 null（除非显式传 null 默认值）
String host = Configs.getStr(ConfigKey.BOT_INTERNET_HOST, null);
int    port = Configs.getInt(ConfigKey.SERVER_PORT);          // 回落声明默认值
boolean on  = Configs.getBool(ConfigKey.BOT_SWITCH_DISABLE_GROUP);
List<Long> admins = Configs.getList(ConfigKey.BOT_SUPERUSERS, Long.class, List.of());
```

要点：

- 快照是 `Collections.unmodifiableMap`，**读无锁**；写入时整体替换引用，读到的要么旧快照要么新快照，不会读到半成品。
- 类首次被访问时（`static` 块）就完成加载，**不依赖 Spring 容器**。
- 加载优先级：**声明默认值 → jar 内同名文件（兜底）→ 外置 `./config/` 文件（最高）→ 同名系统属性**。
- 支持 `haruhibot.xxx=yyy` 形式的带应用名前缀写法。
- `getStrStrict(key, default)`：key 存在时**原样返回（含空串）**，用于"显式清空密码/密钥"这类语义。
- `Configs.isConfigured(key)` / `Configs.source(key)` 让前端区分「已配置」与「正在用默认值」。
- `Configs.save(key, value)` 供**程序自动写入**（如 b站自动获取的 ticket）；yml 类配置不支持写入。

> ⚠️ 唯一约定：**不要在 `static final` 字段初始化时取值**，否则会把启动瞬间的值永久固化。请在方法内部调用。

### 2.4 写值与热更新：`ConfigHub`

`src/main/java/com/haruhi/botServer/config/service/ConfigHub.java`

```
save(key, value)
  → 1. 校验（yml类拒绝写入；ConfigType.validate 校验类型）
  → 2. 写文件（PropertiesFileUtil，保留注释/顺序/空行）
  → 3. 刷新该文件对应的内存快照
  → 4. 仅当 key.hot=true 且值真的变了 → 按 key 通知 ConfigApplier
  → 5. 重建前端展示用的配置项缓存
```

- **key 级刷新**：`refresh(key)` —— 重读该 key 所在文件，不写文件。
- **文件级刷新**：`refreshFile(file)` —— 重读整个文件。
- **全量刷新**：`refreshAll()` —— 兜底按钮。
- **外部改动自动感知**：`@Scheduled(fixedDelay=2000)` 轮询文件最后修改时间，检测到变更自动重载并通知订阅者。

### 2.5 订阅者：`ConfigApplier`

必须持有配置值的组件（连接池、HttpClient、线程池、Quartz Trigger）实现本接口，按 key 精确接收变更：

| 组件 | 订阅的 key | 行为 |
|---|---|---|
| `OpenAiServiceHolder` | `ds.api.key` / `ds.api.base_url` / `ds.api.timeout` | 释放旧 `OpenAiService`，下次调用按新配置重建 |
| `JobManage` | `job.*` 全部 | `enable` 变化 → 即时注册/取消任务；`cron` 变化 → 即时重新排期 |

**只读 `Configs` 的代码不需要实现本接口**——这就是热更新粒度能做到单个 key 的原因。

### 2.6 文件读写

- `PropertiesFileUtil`：刻意**不用** `Properties#store`（会打乱顺序、丢注释），按行解析、按行更新，保留文件头注释、key 上方注释、其他项顺序与空行；新增 key 追加到末尾；重置时删除整行；`\n`、`\r`、`\t`、`\\`、`\uXXXX` 双向转义。
- `YamlFileUtil`：只读，把 yml 的叶子节点**拍平**成 `a.b.c=value`（与 properties 同样的寻址方式），列表用逗号连接，与 `ConfigType.LIST` 对齐。
  **不支持写**：yml 的缩进/锚点/多行块难以在保留格式的前提下安全改写，因此 yml 类配置在页面上只读，需要直接编辑文件并重启。

### 2.7 为什么 `db.sql_cache` 不在配置文件里

`db.sql_cache` 存的是"用户在 WebUI SQL 编辑器里随手写过的SQL"，属于**操作数据而不是配置**，
因此它不进 `./config/`，仍由 `SqlCacheStore` 存放在数据库 `t_dictionary` 表中（与重构前行为一致）。
`ConfigMigrator` 也会跳过它——既不搬迁，也不删除。

---

## 3. WebUI：系统管理 / 配置管理

菜单路径：**系统管理 → 配置管理**（`/config`，`webui/src/views/config/index.vue`，接口 `webui/src/api/config.js`）

**左侧**：配置文件列表（中文名 + 文件名 + 项数 + 可热更新项数）。

**右侧**：

- 头部：大类名、文件名、`重新加载此文件`、`查看文件`、`保存修改（N）`。
- 表格列：
  - **配置项**：中文名 + `key` 小字
  - **值**：按 `type` 渲染控件 —— `BOOL`→开关、`INT`→数字框、`SECRET`→密码框（已设置时显示掩码占位）、`LIST`/`JSON`→多行文本、其他→单行文本；**yml 类配置显示为只读文本 + 锁图标提示**
  - **状态**：`即时生效`（绿）/ `需重启`（橙）+ 值来源
  - **说明**：remark + 默认值对比
  - **操作**：`保存`（仅可写且有改动时可用）、`刷新`（仅 `hot=true` 的行显示，即 **key 级热更新**）、`重置`（回到默认值并删除文件中该行）
- 有改动的行高亮；切换文件时若有未保存改动会提示。

**接口**（`ConfigController`，前缀 `/api/config`）：

| 接口 | 说明 |
|---|---|
| `POST /list` | 全部配置，按文件分组；SECRET 不下发明文，只给 `maskedValue` + `hasValue`；yml 项 `writable=false` |
| `POST /file/content` | 读取某个配置文件的原始文本 |
| `POST /save` | 保存单个配置项 |
| `POST /batchSave` | 批量保存（跨文件也可），返回 `hotKeys` / `restartKeys` |
| `POST /reset` | 重置为声明默认值（并删除文件中的该行） |
| `POST /refresh` | **key 级刷新** |
| `POST /refreshFile` | **文件级刷新** |
| `POST /refreshAll` | 全量重新加载 |

保存响应会明确区分：`已即时生效：xxx，需重启生效：yyy`。

---

## 4. 老配置迁移

`ConfigMigrator` 在启动时（`FirstTask`）把旧版数据库 `t_dictionary` 表里的配置搬到 `./config/*.properties`：

- **支持旧key映射**：`bot.access_token → bot.ws.access_token`、`bot.max_connections → bot.ws.max_connections`、
  `saucenao.search_image_key → searchimg.saucenao.apikey`、`url_conf.agefans → searchimg.agefans.url`、`switch.* → bot.switch.*`，
  配置项改名不会丢配置。
- 只处理**配置文件里还没有**的 key → **幂等，可重复执行**。
- **不属于配置的 key 原样保留**（如 `db.sql_cache`），既不搬迁也不删除。
- 迁移成功的配置会从字典表移除（文件是唯一真源）；失败的保留，下次启动重试。
- 表不存在（全新部署）时直接跳过。

---

## 5. Spring 侧的配合

`application.yml` 只保留日志与 profile：

```yaml
spring:
  profiles:
    active: dev
```

`./config/` 下的配置（含 `server.yml`）通过 `ConfigsEnvironmentInitializer`
（注册于 `src/main/resources/META-INF/spring.factories`）在容器 refresh 之前注入 Spring Environment，
优先级最高（`addFirst`）。因此：

- `@ConditionalOnProperty`（如 `job.*.enable`）、`@Value`、第三方 starter 的属性绑定，
  与 `Configs` 读到的是**同一份配置**；
- 不依赖 `spring.config.import` 的工作目录解析；
- yml 里的 `server.port` 既能被 Spring 消费，也能被 `Configs.getInt(ConfigKey.SERVER_PORT)` 读到。

已全部改为直接读 `Configs`：`BotConfig` 只保留编译期常量，`BilibiliLiveJob`、`DownloadPixivJob`、
`LoginService`、`DruidConfig`、三个 druid `Condition`、各 handler / service 的取值点。
原先的 `DictionarySqliteService`（配置门面）已删除，`getBotSuperUsers()` 之类的能力直接用 `Configs.getList(...)`。

---

## 6. 开发规约

1. **新增配置项**：在 `ConfigKey` 里加一行（选好 `file`、`type`、默认值、`hot`、`remark`），
   然后在对应 `./config/*.properties` 里补上带注释的默认值。WebUI 会自动出现该项，**不需要改前端**。
2. **取值**：一律用 `Configs.getXxx(ConfigKey.XXX[, default])`，不要通过中间服务、不要缓存到 `static final`。
3. **判断能否热更新**：
   - 只在方法内部读 → `hot=true`；
   - 被 bean 构造/条件装配/`static final` 使用 → `hot=false`（WebUI 上如实展示"需重启"）；
   - 持有连接/线程池/触发器 → `hot=true` + 实现 `ConfigApplier`，按 key 重建。
4. **程序自动写入的值**：用 `Configs.save(key, value)`（yml 类会被拒绝）。

---

## 7. 测试

| 测试类 | 覆盖内容 |
|---|---|
| `ConfigsTest`（17 项） | 声明默认值回落、老key反查、保存即时生效并落盘到正确文件、写文件保留注释与顺序、新增 key 追加、多行值转义往返、注释/未声明 key 被忽略、类型校验、**yml 拍平后进入快照**、**yml 拒绝页面写入**、文件级刷新发现外部改动、key 级刷新、重置回默认值并删除文件行、`Configs.save` 程序写入、敏感值脱敏、声明完整性 |
| `ConfigSpringIntegrationTest`（6 项） | **完整应用上下文启动成功**、properties 与 yml 同时被 `Configs` 与 Spring Environment 读到、`ConfigApplier` 订阅者已注册并被通知、接口分组数据完整（含 yml 只读标记）、保存后落盘并可刷新、非热更新项与 yml 项的保存行为、读取文件原文 |

> 注：`ConfigSpringIntegrationTest` 使用 `@NoMockitoSpringTest` 替换默认测试监听器，原因见该注解的 javadoc。

---

## 8. 改动清单（本次二次重构）

**新增**

```
src/main/java/com/haruhi/botServer/config/util/YamlFileUtil.java
src/main/java/com/haruhi/botServer/config/config/ConfigsEnvironmentInitializer.java
src/main/java/com/haruhi/botServer/config/service/SqlCacheStore.java
src/main/resources/META-INF/spring.factories
src/main/resources/config/server.yml
src/main/resources/config/websocket.properties
src/main/resources/config/searchimg.properties
```

**删除**

```
src/main/java/com/haruhi/botServer/service/DictionarySqliteService.java
src/main/resources/config/server.properties        （server.port 迁到 server.yml）
src/main/resources/config/switch.properties        （迁到 bot.properties 的 bot.switch.*）
```

**改造**

```
ConfigFile（支持 yml + 新文件分组）、ConfigKey（新key/legacyKeys/文件调整）、
Configs（yml加载、loadIntoEnvironment、fileOf、save）、ConfigHub（yml拒写、writable、中文名）、
ConfigMigrator（旧key映射 + 保留非配置数据）、BotConfig（只保留常量）、
JobManage / BilibiliLiveJob / DownloadPixivJob / SystemService / SystemController /
各 handler·service（去掉 DictionarySqliteService 与 BotConfig 中转，直接读 Configs）、
AbstractSearchEngine（baseUrl 可热更新）、SauceNao（每次请求取最新接口地址）、
NewAnimationTodayHandler / BtSearchHandler（注释里的旧key同步）
```

**key 命名调整一览**

| 旧key | 新key | 文件 |
|---|---|---|
| `bot.access_token` | `bot.ws.access_token` | websocket.properties |
| `bot.max_connections` | `bot.ws.max_connections` | websocket.properties |
| `bot.same-machine-qqclient` | 不变 | server.properties → **bot.properties** |
| `bot.internet-host` | 不变 | server.properties → **bot.properties** |
| `switch.disable_group` | `bot.switch.disable_group` | switch.properties → **bot.properties** |
| `switch.qingyunke_chat` | `bot.switch.qingyunke_chat` | 同上 |
| `switch.search_image_allow_group` | `bot.switch.search_image_allow_group` | 同上 |
| `switch.search_bt_allow_group` | `bot.switch.search_bt_allow_group` | 同上 |
| `switch.group_increase` | `bot.switch.group_increase` | 同上 |
| `switch.group_decrease` | `bot.switch.group_decrease` | 同上 |
| `saucenao.search_image_key` | `searchimg.saucenao.apikey` | ai.properties → **searchimg.properties** |
| （硬编码） | `searchimg.saucenao.baseurl` | **searchimg.properties（新增）** |
| `url_conf.agefans` | `searchimg.agefans.url` | url.properties → **searchimg.properties** |
| `db.sql_cache` | **移除**（回到数据库存储） | — |
