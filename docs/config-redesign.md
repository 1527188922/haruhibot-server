# haruhibot-server 配置管理方案

> 本文档描述 **已落地** 的配置管理实现。
> 一句话概括：**配置文件是配置的唯一真源（配置不进数据库），按文件分组，通过 `Configs` 静态快照读取、`ConfigHub` 写入与热更新通知，WebUI「系统管理 / 配置管理」页按文件分组编辑。所有配置项都可编辑，区别只在"改完是否立即生效"。**

---

## 1. 设计目标

| 原始痛点 | 本方案的解决方式 |
|---|---|
| 改完不需要重启就能生效 | 每个配置项声明 `hot` 属性；`hot=true` 时改完**立即生效**（写文件 → 换内存快照 → 按 key 通知订阅者） |
| `application.yml` 里的配置 | `bot.*` / `job.*` 搬进 `./config/*.properties`；`application.yml`（**只有这一份 yml**）也纳入配置管理（同一个页面里编辑），`server.port` / 日志级别等仍由 Spring 直接消费 |
| `config` 目录下的 properties | 从"第二套体系"升级为**唯一真源**，并按业务拆成多个文件分组 |
| 静态方法中也要能用 | `Configs` 在**类加载时**就加载好不可变快照，不依赖 Spring，静态块 / 静态方法 / 构造器中调用都安全，且**永不返回 null** |
| 需要 WebUI 方便编辑 | 「系统管理 / 配置管理」页：按文件分组、类型化控件、key 级刷新、文件级整体刷新；**不存在不可编辑的项** |

---

## 2. 总体结构

```
                    ┌──────────────────────────────────────────────┐
                    │  程序目录/                                    │
   程序启动         │    application.yml   ← 唯一一份 yml           │
   （不依赖Spring） │      （端口 / 日志级别）                       │
        │           │    config/                                   │
        │           │      webui.properties                        │
        │           │      job.properties      bot.properties      │
        │           │      websocket.properties                    │
        │           │      searchimg.properties                    │
        │           │      bilibili.properties ai.properties       │
        │           │      jm.properties       url.properties      │
        │           │      database.properties chat_record.properties│
        │           └───────────────┬──────────────────────────────┘
        │                           │ 启动时 / 变更时读取（properties + yml）
        ▼                           ▼
  Configs（静态快照，不可变 Map，volatile 整体替换）  ◄──── ConfigHub（写文件 + 刷新快照 + 通知）
        │                                                      ▲
        │ 静态读取（任意时机、无锁、永不null）                    │ WebUI / 定时监听外部改动
        ▼                                                      │
  业务代码（handler / service / job / condition …）      ConfigApplier 订阅者
                                                          （OpenAiServiceHolder、JobManage …）
```

**环境维度不再靠 profile / 多份 yml**：以前 `application-dev.yml` / `application-prod.yml` 配合 Spring profile 区分环境
（`ProdEnvironmentCondition` 的判定条件是"没有激活的 profile"），现在只有一个 `application.yml`，
环境差异只体现在日志级别这类配置项上，部署包也只带这一份 yml。

### 2.1 配置声明：`ConfigKey`

`src/main/java/com/haruhi/botserver/configuration/metadata/ConfigKey.java` 是**唯一声明处**：

| 字段 | 说明 |
|---|---|
| `file` | 所属 `ConfigFile`（决定落在哪个文件、前端归到哪个大类） |
| `key` | 配置key，同时作为 Spring 属性名 |
| `type` | **值类型**：`STRING / INT / BOOL / LIST / JSON / SECRET`，决定保存校验、yml 标量是否加引号、读取时的反序列化 |
| `defaultValue` | 缺省值。配置缺失时一律回落到它 |
| `hot` | **是否可热更新**。false 表示该项在启动阶段就已确定，改完需要重启 |
| `remark` | 说明，前端展示 |
| `sort` | 前端排序 |
| `control` | **前端控件**（`ControlMeta`，可省略）。不写就按值类型取默认控件 |

查找方式：

- `ConfigKey.of(key)` —— 按属性名反查
- `ConfigKey.of(file, key)` —— 按"文件 + 属性名"反查（机制保留：万一以后同一属性名出现在多个文件里，可精确定位）
- `ConfigKey.isAmbiguous(key)` —— 该属性名是否被多个文件声明（当前没有任何 key 是这种，dev/prod 文件已删）
- `ConfigKey.of(file)` —— 某个文件下的全部配置项

#### 值类型 ≠ 控件类型：`ControlType` / `ControlMeta`

**值类型管"存什么、怎么校验"，控件类型管"页面上用什么控件"**，两者解耦：

| | 类 | 取值 |
|---|---|---|
| 值类型 | `ConfigType` | `STRING / INT / BOOL / LIST / JSON / SECRET` |
| 控件类型 | `ControlType` | `INPUT` / `SWITCH` / `SELECT` / `CHECKBOX` / `RADIO` |

同一个值类型可以配不同控件（`STRING` 既可以是输入框，也可以是下拉/单选组/复选组；
`BOOL` 既可以是开关，也可以是单个复选框），控件再由 `ControlMeta` 描述候选项等细节：

| ControlMeta 字段 | 说明 |
|---|---|
| `type` | 控件类型 |
| `options` | 候选项 `[{value,label}]`，声明时写成 `"值:显示名,值:显示名"`（冒号可省略，省略时显示名=值） |
| `multiple` | 是否多选（`SELECT` / `CHECKBOX`） |
| `allowCustom` | 是否允许自定义值（下拉可输入候选项之外的值） |

不声明时的默认控件：`BOOL → SWITCH`，`LIST → SELECT`（多选 + 允许自定义值，即标签输入框），
其余（`STRING / INT / SECRET / JSON`）→ `INPUT`（前端再按值类型细分数字框、密码框、多行文本）。

下拉（`SELECT`）的三种模式都用 `multiple` / `allowCustom` 表达：

| 模式 | 写法 |
|---|---|
| 单选 | `ControlMeta.select("a:A,b:B")` |
| 多选 | `ControlMeta.selectMultiple("a:A,b:B")` |
| 可自定义值 | `ControlMeta.selectCustom(true/false, "a:A")`（候选可以为空，纯标签输入框） |

声明示例（`ConfigKey` 里追加一个参数即可）：

```java
// 日志级别：单选下拉
LOGGING_LEVEL(..., "info", false, "本项目自身的日志级别…", 20,
        ControlMeta.select("TRACE,DEBUG,INFO,WARN,ERROR")),
// 浏览器下载模式：单选组
PLAYWRIGHT_...(..., "0", true, "浏览器强制下载模式…", 50,
        ControlMeta.radio("0:自动判断,1:强制跳过下载（用系统浏览器）,2:使用playwright下载的浏览器")),
// druid 过滤器：复选组（多值，逗号拼接）
DATABASE_DRUID_FILTERS(..., ConfigType.STRING, "stat", false, "druid过滤器…", 40,
        ControlMeta.checkboxGroup("stat,wall,log4j,slf4j")),
// 单个复选框（BOOL）
X(..., ConfigType.BOOL, "false", true, "说明", 10, ControlMeta.checkbox()),
```

前端的取值转换：多值控件（多选下拉、复选组）在页面上是数组模型，保存时用逗号拼成一个字符串写入
（与 `ConfigType.LIST` 的解析方式一致）；候选项之外、但当前值里已有的值会在页面上补成候选项显示，
避免"看不到已配置的值、一保存就丢掉"。

### 2.2 文件分组：`ConfigFile`

两类位置、两种格式，都是配置管理页的一等公民：

| 文件 | 中文名 | 内容 |
|---|---|---|
| `application.yml` | 应用主配置 | `server.port`、`logging.level.com.haruhi.botserver`（**需重启**） |
| `webui.properties` | WebUI | 登录账号密码、JWT、会话、druid监控台开关（**需重启**） |
| `job.properties` | 定时任务 | 各任务 `enable` / `cron`（**开关与cron均可即时生效**） |
| `bot.properties` | 机器人 | 同机部署、对外地址、超级管理员、可访问群、上传并发、`bot.switch.*` 功能开关 |
| `websocket.properties` | WebSocket | `bot.ws.access_token`、`bot.ws.max_connections` |
| `searchimg.properties` | 识图 | `searchimg.saucenao.baseurl`、`searchimg.saucenao.apikey` |
| `bilibili.properties` | B站 | cookie、上传/下载时长限制 |
| `ai.properties` | AI | 千问key、DeepSeek key/baseurl/timeout |
| `jm.properties` | JM漫画 | 解压密码、线程数、并发、名称长度、API域名、搜索结果 |
| `url.properties` | 站点地址 | 第三方站点与接口地址（含原 `ThirdPartyURL` 常量迁移过来的项） |
| `database.properties` | 数据库 | **数据源唯一真源**：jdbc url、驱动、druid 参数（**需重启**，会覆盖 yml 里的 `spring.datasource`） |
| `chat_record.properties` | 聊天记录 | `db.chat_extend.raw_compress` |

`ConfigFile.order` 决定加载优先级（**大的覆盖小的**）：

```
application.yml (10)  <  ./config/*.properties (30)
```

`ConfigFile.isSpringApplicationFile()` 用于区分 yml 的存放位置（程序目录 vs `config/`）。

**dev/prod 怎么区分**：不区分了。

- 日志级别只有一份：`logging.level.com.haruhi.botserver`（原来分 dev=debug / prod=info 两份文件，现在一份，默认 `info`）。
- 静态资源地址只有一套实现：`WebResourceConfig`（`config/webResource` 包）。原来 `AbstractWebResourceConfig` +
  `DevWebResourceConfig` / `ProWebResourceConfig` 两个实现，靠 Spring profile（`ProdEnvironmentCondition` 判定
  "没有激活的 profile"注册 prod 版）和 `webui.dev-mode` 配置来选，现在全部删掉，只留一个类、无条件装配。
  `setWebHomePath()` 用原 prod 的取值顺序：配置的 `bot.internet-host` → 自动探测公网IP → 内网IP。
- 之所以能合并：**资源目录结构与部署目录一致**了。`image/`、`audio/`、`customReply/`、`script/` 已从
  `src/main/resources/build/**` 搬到 `src/main/resources/**`（与 `config/`、`templates/` 同级，`build/` 里只剩 `package.xml`），
  打包后仍是部署目录根下的 `image/`、`audio/` …，而 IDE 里跑时它们在 `target/classes/image` 等位置，
  由 `WebServletConfig` 的 `/**` 映射统一对外提供——因此不再需要 `/build/**` 这种 dev 专用前缀。

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

- 快照是 `Collections.unmodifiableMap`，**读无锁**；写入时整体替换引用，读到的要么旧快照要么新快照。
- 类首次被访问时（`static` 块）就完成加载，**不依赖 Spring 容器**。
- 加载优先级：**声明默认值 → jar 内同名文件（兜底）→ 外置文件（按 order 覆盖）→ 同名系统属性**。
- 支持 `haruhibot.xxx=yyy` 形式的带应用名前缀写法。
- `getStrStrict(key, default)`：key 存在时**原样返回（含空串）**，用于"显式清空密码/密钥"这类语义。
- `Configs.isConfigured(key)` / `Configs.source(key)` 让前端区分「已配置」与「正在用默认值」。
- `Configs.save(key, value)` 供**程序自动写入**（如 b站自动获取的 ticket）。
- `Configs.fileOf(file)` / `pathOf(file)` 返回配置文件的真实位置（yml 在程序目录，properties 在 `config/`）。

> ⚠️ 唯一约定：**不要在 `static final` 字段初始化时取值**，否则会把启动瞬间的值永久固化。请在方法内部调用。

### 2.4 写值与热更新：`ConfigHub`

`src/main/java/com/haruhi/botserver/configuration/service/ConfigHub.java`

```
save(key, value)
  → 1. 校验类型（ConfigType.validate）
  → 2. 写文件（properties 按行改；yml 按行原地改值）
  → 3. 刷新该文件对应的内存快照
  → 4. 仅当 key.hot=true 且值真的变了 → 按 key 通知 ConfigApplier
  → 5. 重建前端展示用的配置项缓存
```

- **key 级刷新**：`refresh(key)` —— 重读该 key 所在文件，不写文件。
- **文件级刷新**：`refreshFile(file)` —— 重读整个文件。
- **全量刷新**：`refreshAll()` —— 兜底按钮。
- **重置**：`resetAll(keys)` —— 把每个 key 的值改回声明中的默认值（**保留该 key**），走的还是同一套写文件逻辑，
  因此既不会重复追加，也不会在 yml 里写成 properties 风格的点分行。
- **刷新结果会区分三种情况**（`ConfigRefreshResult{fileChanged, changes}`），否则"我明明改了文件，为什么说无变化？"很费解：
  1. 文件内容与内存快照一致（`fileChanged=false`、无差异）→ "已是最新（文件内容与当前配置一致；若刚改过文件，改动已被自动重载并生效）"；
  2. 文件内容变了、但声明的配置值没变（`fileChanged=true`、无差异）→ "已重新读取，但声明的配置值没有变化（可能只改了注释、空行或未声明的key）"；
  3. 声明的配置值变了 → 报出变化的 key 与是否即时生效。
  > 第 1 种是**最常见**的：下面这条自动重载通常 2 秒内就把手工改的文件生效了，此时再点刷新自然"没有变化"。
- **外部改动自动感知**：`@Scheduled(fixedDelay=2000)` 轮询，比较的是**文件内容摘要（SHA-256）**而不是最后修改时间
  ——时间戳精度（Windows 约 15ms）会把同一刻的两次写入漏掉，内容摘要不会。启动时（`@PostConstruct`）先记住各文件的摘要，
  因此从第一秒起就能感知外部改动。

**热更新的触发条件是"值真的变了"**：`ConfigApplier.onlyOnChanged()` 默认 true，同一个值再保存一次不会产生通知
（例如 `job.*.enable` 已经是 true 时再保存 true，不会重复注册任务，因为任务本来就在跑）。
如果运行期状态和文件出现了偏差、"保存了却没反应"，用配置页的 **`重新加载全部配置`**（`refreshAll` → `fireAll`）强制给每个订阅者发一次通知即可对齐。

**没有"不可编辑"的配置项**：yml 也只是"改完需要重启"，页面一样可以保存与重置。

### 2.5 订阅者：`ConfigApplier`

必须持有配置值的组件（连接池、HttpClient、线程池、Quartz Trigger）实现本接口，按 key 精确接收变更：

| 组件 | 订阅的 key | 行为 |
|---|---|---|
| `OpenAiServiceHolder` | `ds.api.key` / `ds.api.base_url` / `ds.api.timeout` | 释放旧 `OpenAiService`，下次调用按新配置重建 |
| `JobManager` | `job.*` 全部 | `enable` 变化 → 即时注册/取消任务；`cron` 变化 → 即时重新排期 |
| `WebResourceConfig` | `internet-host` | 重算对外访问地址（端口需重启） |
| `PlaywrightBrowserModeApplier` | `playwright.skip-browser-download-mode` | 清掉 `HtmlToImageUtils` 里"要不要跳过浏览器下载"的缓存，下一次截图按新配置重新决策（模式一旦探测出来就会固定，否则每次截图都可能重跑一遍长达 10 分钟的浏览器安装） |

**只读 `Configs` 的代码不需要实现本接口**——这就是热更新粒度能做到单个 key 的原因。
反过来，像上面两个"缓存了值/决策"的组件就**必须**实现本接口，把缓存清掉或重算，否则配置改了也不会生效。

> 新增一个这样的订阅者只需三步：类上加 `@Component` 并实现 `ConfigApplier` → `keys()` 返回关心的 key
> → `onConfigChange()` 里清缓存/重算；然后把对应 `ConfigKey` 的 `hot` 改成 `true`
> （**hot=false 的项不会被通知**，页面也会显示"需重启"）。

### 2.6 properties 读写：`PropertiesFileUtil`

刻意**不用** `Properties#store`（会打乱顺序、丢注释），按行解析、按行更新：

- 更新已存在的 key：只替换那一行，保留文件头注释、key 上方注释、其他项顺序与空行。
- 新增 key：追加到文件末尾。
- 重置：把默认值写回该 key（保留该行，不删除）。
- 值转义：`\n`、`\r`、`\t`、`\\`、`\uXXXX` 双向转换。
- 统一用 `\n` 写回（不能按平台用 `\r\n`，否则 `readAllLines` 会把 `\r` 留在行内容里）。

### 2.7 yml 读写：`YamlFileUtil`

**读**：把嵌套结构拍平成 `a.b.c=value`（与 properties 同样的寻址方式），只保留叶子节点；列表用逗号连接，与 `ConfigType.LIST` 对齐。

**写**：按行定位后**原地替换**，保留注释、缩进与键顺序。定位方式不是"把 key 按 `.` 切开再逐段找行"，而是先把文件按缩进解析成
"行 → 该行拍平后的完整路径"，再按路径精确匹配（`logging.level.com.haruhi.botserver` 这种**最后一段本身带点**的 key 也能命中）。

- 键已存在（嵌套写法或 `a.b.c: value` 点分写法都算）→ 只替换该行的值（保留行尾注释），并顺手清掉历史上被重复追加的行：**一个 key 只保留一行**
- 只有点分写法（properties 风格）→ 删掉旧行，按 yml 嵌套结构重写
- 父节点存在、键不存在 → 插入到父级块末尾，缩进与同级一致，剩余路径作为点分叶子（如 `logging: level:` 下写 `com.haruhi.botserver: debug`）
- 父节点也不存在 → 文件末尾补出第一层块 + 点分叶子（`server:` / `"  port: 8090"`、
  `logging:` / `"  level.com.haruhi.botserver: debug"`），**不再**写成 `server.port: 8090` 这种平铺行，
  也不会把 `com.haruhi.botserver` 这种 logger 名拆成假层级
- 重置（`reset`）走的是同一套写值逻辑，只是把值换成默认值：**保留该 key**，不会删行
- 标量按需加引号：`ConfigType.INT / BOOL` 写裸值（`port: 8090`），字符串类型需要时加双引号（避免被解析成布尔/数字）

之所以不用 `Yaml.dump()` 整体重写：那样会丢掉所有注释、打乱顺序，对 `application.yml` 这类人也要看的文件不可接受。

> **历史遗留数据的自愈**：旧版本的定位方式（按点切分）对上面那类 key 永远定位失败，于是每次保存都往文件末尾追加一条 `a.b.c: value`。
> 重复的 key 会让 Spring 的 yml 加载器直接抛异常（`found duplicate key xxx`），应用起不来，而配置文件又只能用配置管理页修改——
> 起不来就改不了。为此写了 `YamlDuplicateKeyRepairPostProcessor`（`EnvironmentPostProcessor`，order = `HIGHEST_PRECEDENCE`，
> 早于 `ConfigDataEnvironmentPostProcessor` 解析 `application.yml`）：启动最早期把重复的 key 去重，同一路径只保留**最后一行**
> （旧的保存逻辑总是往末尾追加，最后一行才是最新的值），保留下来的若是点分写法会一并改写成嵌套结构。
> **注意：该钩子与它在 `spring.factories` 里的注册目前是注释状态**（写值逻辑已修好，正常情况不会再产生重复行；
> 若哪天 yml 又被写坏、应用起不来，把它恢复即可自愈）。


### 2.8 为什么 `db.sql_cache` 不在配置文件里

`db.sql_cache` 存的是"用户在 WebUI SQL 编辑器里随手写过的SQL"，属于**操作数据而不是配置**，
因此它不进配置文件，由 `SqlCacheStore` 存放在数据库 `t_dictionary` 表中（与重构前行为一致）。

---

## 3. WebUI：系统管理 / 配置管理

菜单路径：**系统管理 → 配置管理**（`/config`，`webui/src/views/config/index.vue`，接口 `webui/src/api/config.js`）

**左侧**：配置文件列表（中文名 + 文件名 + 项数 + 可热更新项数），按加载顺序排列。

**右侧**：

- 头部：大类名、文件名、`重新加载此文件`、`查看文件`、`保存修改（N）`
- 表格列：
  - **配置项**：中文名 + `key` 小字
  - **值**：按后端下发的 `control`（`ControlType` + 候选项）渲染 —— `INPUT`→输入框（`INT` 数字框、`SECRET` 密码框、`LIST`/`JSON` 多行文本）、`SWITCH`→开关、`SELECT`→下拉（单选/多选/可自定义值）、`CHECKBOX`→复选组或单个复选框、`RADIO`→单选组
  - **状态**：`即时生效`（绿）/ `需重启`（橙）+ 值来源
  - **说明**：remark + 默认值对比
  - **操作**：`保存`（有改动时可用）、`刷新`（仅 `hot=true` 的行显示，即 **key 级热更新**）、`重置`
- 有改动的行高亮；切换文件时若有未保存改动会提示

**接口**（`ConfigController`，前缀 `/api/config`）：

| 接口 | 说明 |
|---|---|
| `POST /list` | 全部配置，按文件分组；SECRET 不下发明文，只给 `maskedValue` + `hasValue` |
| `POST /file/content` | 读取某个配置文件的原始文本（properties 与 yml 都支持） |
| `POST /save` | 保存单个配置项 |
| `POST /batchSave` | 批量保存（跨文件也可），返回 `hotKeys` / `restartKeys` |
| `POST /reset` | 重置为声明默认值（把默认值写回该 key，保留该行；yml 会顺手把历史的点分重复行清理成一个嵌套行） |
| `POST /refresh` | **key 级刷新**（返回提示，说明是无变化/只改了注释/具体哪些key生效） |
| `POST /refreshFile` | **文件级刷新**（同上） |
| `POST /refreshAll` | 全量重新加载 |

保存响应会明确区分：`已即时生效：xxx，需重启生效：yyy`。

---

## 4. Spring 侧的配合

只有一份 `application.yml`：Spring Boot 自己就会加载它（classpath / 程序目录），端口与日志级别都由此生效，
`Configs` 也从同一份文件读快照，两边不会出现"两份配置谁生效"。

`./config/*.properties` 只通过 `Configs` 快照被业务代码读取（`@Value` / `@ConditionalOnProperty` / 第三方 starter
不再从 Environment 里拿它们——`ConfigsEnvironmentInitializer` 目前**注释掉未启用**，因为业务代码已全部改为直接读 `Configs`，
`JobManage`、`DruidConfig`、各 Condition 用的都是静态快照）。如果以后要让某个 starter 用 properties 里的值，
把 `ConfigsEnvironmentInitializer` 与它在 `spring.factories` 里的注册恢复即可（它的作用是把
`application.yml` + `./config/*.properties` 以最高优先级 `addFirst` 注入 Environment）。

已全部改为直接读 `Configs`：`BotConfig` 只保留编译期常量（`CONTEXT_PATH`、`DRUID_PATH`、`WEB_SOCKET_PATH`、`DEFAULT_NAME`），
`BilibiliLiveJob`、`DownloadPixivJob`、`LoginService`、`DruidConfig`、三个 druid `Condition`、
各 handler / service 的取值点。原先的 `DictionarySqliteService`（配置门面）已删除。

---

## 5. 从旧版本升级

**没有自动迁移**。旧版本存在数据库 `t_dictionary` 表里的配置不会被读取，也不会被改写，
升级后请按本文档第 8 节的对照表，在配置管理页（或直接编辑 `./config/` 下的文件）手动把需要的值填一遍：

- 数据库里那份旧配置会原样留着，可以随时对照；确认不需要后自行清理。
- 新配置文件首次启动时会写入声明里的默认值，不改就是走默认值。
- `db.sql_cache` 属于操作数据，仍然只存在数据库里（见 §2.8），不受影响。

---

## 6. 开发规约

1. **新增配置项**：在 `ConfigKey` 里加一行（选好 `file`、`type`、默认值、`hot`、`remark`，需要特定控件时再加 `control`），
   然后在对应文件里补上带注释的默认值。WebUI 会自动出现该项，**不需要改前端**。
2. **取值**：一律用 `Configs.getXxx(ConfigKey.XXX[, default])`，不要通过中间服务、不要缓存到 `static final`。
3. **判断能否热更新**：
   - 只在方法内部读 → `hot=true`；
   - 被 bean 构造/条件装配/`static final` 使用 → `hot=false`（WebUI 上如实展示"需重启"）；
   - 持有连接/线程池/触发器 → `hot=true` + 实现 `ConfigApplier`，按 key 重建。
4. **同一个属性名要不要放多个文件**：现在没有这种 key（dev/prod 已合并）。机制仍保留：真需要时两个文件各声明一个
   `ConfigKey`，接口调用时带上 `fileName`（`ConfigKey.isAmbiguous` 会提示）。
5. **写 `application.yml` 的注释请用 ASCII**：`maven-resources-plugin` 复制资源时会按平台默认编码处理，
   非 ASCII 注释在该环节会被打乱（这是构建链路的问题，不是配置模块的问题）。中文说明写在 `ConfigKey.remark` 里，
   配置页会展示。

---

## 7. 测试

| 测试类 | 覆盖内容 |
|---|---|
| `ControlMetaTest`（5 项） | **值类型与控件类型解耦**：默认控件映射（BOOL→开关、LIST→多选下拉、其余→输入框）、同一值类型配不同控件、下拉的单选/多选/可自定义值三种模式、`"值:显示名"` 候选项解析、现有配置项声明了预期控件（日志级别下拉、下载模式单选组、druid过滤器复选组、superusers 多选+自定义） |
| `ConfigsTest`（36 项） | 声明默认值回落、未声明key无法反查、properties 保存并落盘、写文件保留注释与顺序、新增 key 追加、多行值转义往返、注释/未声明 key 被忽略、类型校验、**yml 拍平进快照**、**yml 原地改值保留注释与缩进**、**yml 插入/删除键**、**带点key反复保存不新增行**、**历史点分重复行的清理与改写**、**坏文件自动去重**、**重置写回默认值且不新增行**、**日志级别在同一份 yml 且无歧义**、**文件级刷新能发现外部改动且二次刷新回报"文件没变"**、**只改注释时回报文件变了但配置值没变**、key 级刷新、重置、程序写入、标量引号规则、加载顺序、数据源配置读取、文件归属校验 |
| `ConfigSpringIntegrationTest`（21 项） | **完整应用上下文启动成功**、properties 与 yml 同时被 `Configs` 与 Spring Environment 读到、**数据源以 database.properties 为准（真实取一条连接）**、`ConfigApplier` 订阅者已注册并被通知、**playwright 浏览器下载模式热更新（真实断言缓存被清空）**、**job.properties 的 enable/cron 热更新（真实断言 Quartz 触发器注册/取消/重新排期）**、**文件里缺少该 key 时保存后同样能热更新**、**直接用编辑器改文件也会被自动感知并热更新**、**手工改文件后点刷新会报出变化的配置项**、**改动已被自动重载后点刷新会说明原因（"已是最新…自动重载"）而不是含糊的"无变化"**、接口分组数据完整、**配置项下发控件元数据**、保存 properties / yml 后落盘并保留注释、**缺少的 yml key 写成嵌套结构而不是点分行**、**重置接口写回默认值而不是删行**、**日志级别没有歧义且能保存**、**静态资源地址只注册一个实现**、所有配置文件都有可编辑项、读取文件原文 |
| `BotTest`（3 项） | 上传文件并发/排队行为（配置读取已改为 `Configs`） |

> 注：`ConfigSpringIntegrationTest` 使用 `@NoMockitoSpringTest` 替换默认测试监听器，原因见该注解的 javadoc。

---

## 8. 配置项与文件对照

| key | 文件 | 热更新 |
|---|---|---|
| `server.port`、`logging.level.com.haruhi.botserver` | application.yml | 否 |
| `internet-host`、`same-machine-qqclient`、`playwright.skip-browser-download-mode` | application.yml | **是** |
| `spring.datasource.dynamic.datasource.master.*` | database.properties | 否 |
| `login.*`、`druid.*` | webui.properties | 否 |
| `job.downloadPixiv.*`、`job.bilibiliLive.*` | job.properties | **是** |
| `bot.same-machine-qqclient`、`bot.internet-host`、`bot.superusers`、`bot.access_groups`、`bot.upload_file.parallel` | bot.properties | **是** |
| `bot.switch.*` | bot.properties | **是** |
| `bot.ws.access_token`、`bot.ws.max_connections` | websocket.properties | **是** |
| `searchimg.saucenao.baseurl`、`searchimg.saucenao.apikey` | searchimg.properties | **是** |
| `bilibili.*` | bilibili.properties | **是** |
| `qianwen.api_key`、`ds.api.*` | ai.properties | **是** |
| `jm.*` | jm.properties | **是** |
| `url_conf.*`（站点与接口地址，含 agefans / bt搜索 / bt影视 / 青云客 / b站各接口 / lolicon / whatslink / 网易） | url.properties | **是** |
| `db.chat_extend.raw_compress` | chat_record.properties | **是** |

**本次重构中改名或换文件的 key**（供手动对照，无自动迁移）

| 旧版本位置 | 现在 |
|---|---|
| `bot.access_token`（字典表） | `bot.ws.access_token`（websocket.properties） |
| `bot.max_connections`（字典表） | `bot.ws.max_connections`（websocket.properties） |
| `switch.*` 六项（字典表） | `bot.switch.*`（bot.properties） |
| `saucenao.search_image_key`（字典表） | `searchimg.saucenao.apikey` |
| `searchimg.agefans.url`（上一版中间态） | `url_conf.agefans`（url.properties） |
| `db.chat_extend.raw_compress`（db.properties） | 同名，文件改为 chat_record.properties |
| `db.sql_cache`（字典表） | 不迁移，仍在数据库里（操作数据） |
| `server.yml`（上一版中间态） | 取消，端口回到 `application.yml` |
| `application-dev.yml` / `application-prod.yml` + Spring profile（dev/prod 靠"有没有激活profile"判定） | 取消，合并成一份 `application.yml`（日志级别用 `logging.level.com.haruhi.botserver`）；`AbstractWebResourceConfig` + `DevWebResourceConfig`/`ProWebResourceConfig` 也取消，只留一个 `WebResourceConfig`（资源目录 `build/**` 已搬到 resources 根下，与部署目录结构一致）；构建侧 `package.xml` 不再按 maven profile 选文件 |
| `ThirdPartyURL` 常量类 | 各地址变成 `url_conf.*` 配置项（识图那个已存在，未重复搬迁） |

---

## 9. 数据源为什么由 `SqliteDataSourceInitAspect` 接管

`application.yml` 里**不再配置** `spring.datasource`，数据源以 `./config/database.properties` 为唯一真源：

`SqliteDataSourceInitAspect.loadDataSourcesBefore` 在 `YmlDynamicDataSourceProvider.loadDataSources()` 之前执行，
用 `Configs` 快照里的值**覆盖 master 数据源**（url、driver、druid 各项），因此：

- yml 里就算写了 `spring.datasource`，也不会生效——不会出现"两份配置谁生效"的困惑；
- 数据源配置与业务配置一样集中在 `./config/` 下、能在配置管理页看到与编辑；
- 覆盖只作用于 `master`，以后用 yml 配别的数据源不受影响。

`createDataSourceBefore` 仍负责在建连接前创建 sqlite 库文件及其目录。
