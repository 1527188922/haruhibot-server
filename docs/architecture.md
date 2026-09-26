# 项目结构与开发约定

本项目保留单 Maven 模块和独立 WebUI，以业务功能聚合代码。Java 根包为 `com.haruhi.botserver`。

## 仓库目录

```text
src/main/java/com/haruhi/botserver/  后端应用
src/main/resources/                默认配置、模板、数据库资源
src/main/resources/mapper/          按所属模块组织的 MyBatis XML
src/test/java/com/haruhi/botserver/  对应模块的测试；samples 为教学代码
src/assembly/package.xml           发布包描述
scripts/                           构建脚本
scripts/runtime/                   发布包中的启动、停止及服务脚本
webui/src/                         WebUI 应用
webui/examples/                    不参与生产构建的演示页面
docs/                              架构、配置说明及迁移记录
```

根目录构建脚本是兼容入口，委托 `scripts/` 执行。运行脚本仍被打包到发布目录根部，发布目录及 HTTP API 保持原有布局。`target/`、`logs/`、`webui/dist/`、`webui/node_modules/` 是生成物。

## 后端模块

| 包 | 职责 |
| --- | --- |
| `bootstrap` | Spring 装配、启动任务、Web 资源配置及日志 key 兼容 |
| `bot.handler` | 消息与通知处理器契约、处理顺序 |
| `bot.dispatch` | 消息分发及生命周期编排 |
| `bot.session` | Bot 会话、容器和发送能力 |
| `integration.onebot` | WebSocket 接入和 OneBot 协议对象 |
| `configuration` | 配置元数据、文件存储、读写服务、管理接口 |
| `features.<功能>` | 功能自身的入口、业务、外部客户端、持久化和模型 |
| `administration` | 登录、运行状态、系统日志查询、文件与数据库管理 |
| `infrastructure` | 数据源、日志、调度、缓存、图片、媒体、Excel、线程池 |
| `shared` | 公共响应、错误、注解、常量及现存通用工具 |

功能包包括 `bilibili`、`chatrecord`、`imagesearch`、`ai`、`music`、`news`、`jmcomic`、`pixiv`、`reply`、`wordstrip`、`contacts`、`notification` 等。

以 `features.bilibili` 为例：

```text
controller/          Web 管理入口
handler/             Bot 消息入口
service/             业务协调
client/model/        外部接口响应
persistence/entity/  数据库对象
persistence/mapper/  MyBatis Mapper
job/                 定时任务入口
model/               功能请求、响应、枚举
support/             功能专用辅助代码
```

小功能只建立实际需要的目录，不创建空层。数据库实体与接口响应分开；外部服务的响应对象放在客户端内。直接表达 SQLite 持久化能力的既有类保留 `Sqlite` 名称，避免与业务服务混淆。

## 依赖与新增功能

### 通用键值存储

`infrastructure.kvstore` 提供 SQLite 键值存储，供管理界面及其他模块共同使用：

```text
infrastructure/kvstore/
├── service/KvStoreService.java
├── model/KvQuery.java
└── persistence/
    ├── entity/KvEntry.java
    └── mapper/KvEntryMapper.java

administration/dictionary/controller/DictionaryController.java
```

字典管理 Controller 和 `infrastructure.cache.SqlCacheStore` 都调用 `KvStoreService`，不跨层访问 Mapper。管理接口继续使用 `/api/dict` 和原有 JSON 字段；存储仍映射 `t_dictionary`，无需迁移已有数据。`db.sql_cache` 保持原有键名。

当前表允许同一个 key 存储多行，`add` 追加、`getOne` 按修改时间取最新一行、`put` 更新该 key 的已有行或插入新行。带备注的 `put` 仅在插入时设置备注。缓存是服务实例内的完整快照，显式调用 `refreshCache()` 更新，写数据库不会自动刷新缓存。新增调用方应通过服务访问，不能把它当成具有唯一键约束或原子并发 upsert 的数据库。

### WebUI 实时消息总线

WebUI 与后端之间除 HTTP 外还有一条全局 WebSocket：`/api/webui/ws`（`SysConstants.WEBUI_WEB_SOCKET_PATH`），与机器人反向 ws `/api/ws` 相互独立，各自使用自己的握手拦截器与鉴权方式。

```text
infrastructure/web/websocket/
├── WebuiWsHandler.java               连接生命周期与消息分发
├── WebuiWsHandshakeInterceptor.java  用登录token校验握手(?token=)
├── WebuiWsSessionRegistry.java       会话注册表、按主题广播、登出踢线
├── WebuiWsMessage.java               消息信封 {type,id,ts,message,data}
├── WebuiWsCloseCodes.java            自定义关闭码(4001登录态失效/4002登出)
├── WebuiWsCommandHandler.java        客户端命令扩展点
├── WebuiWsCommandContext.java        命令上下文(reply/error/subscribe)
└── WebuiWsTopicProvider.java         订阅后补发首帧的扩展点
```

功能模块实现 `WebuiWsCommandHandler`（处理前端发来的命令）与 `WebuiWsTopicProvider`（订阅后补发首帧、按变化主动推送）即可接入，基础设施层不反向依赖功能包。现有实现是 `features.jmcomic.service.JmTaskPushService` 与 `JmTaskWsCommandHandler`：JM任务队列变化触发通知，推送服务按 300ms 节流并比对快照签名后只广播有变化的快照。

前端单例客户端为 `webui/src/api/ws-client.js`，登录后建立一条连接，页面/组件通过 `this.$ws` 的 `on`/`send`/`subscribe` 使用，断线自动重连并恢复订阅；服务端判定登录态失效时以 4001 关闭，前端按 401 的流程退出登录。`WebSocketSession#sendMessage` 不是线程安全的，所有会话统一包一层 `ConcurrentWebSocketSessionDecorator`。HTTP 接口保持不变，WebSocket 未连通时前端回退到轮询。

注意：浏览器 WebSocket 不能自定义请求头，token 只能放在查询参数里；MVC 的 `ApiHeaderInterceptor` 不作用于 WebSocket 握手，因此必须由握手拦截器单独鉴权。

### 模块约定

1. Controller、Handler、Job 调用业务服务。功能自己的 Mapper、DTO、常量和工具随功能存放。
2. 消息分发器只依赖处理器契约。后台处理器通过 `bypassGroupRestrictions()` 声明是否绕过群命令开关和群访问列表；聊天记录保持原有放行行为，自发消息判断仍独立生效。
3. 处理器注册表属于分发器实例，不能在静态字段里跨 Spring 容器累积。
4. 跨功能协作优先通过服务，避免新增对其他功能 Mapper 的直接调用。`shared` 不得引入具体功能依赖。
5. 包名全小写；Java 类型使用准确的 PascalCase 名称。不要新增顶层 `service`、`dto`、`vo` 或业务专用 `utils`。
6. 每次搬包都同步检查 MyBatis namespace/resultType、AOP 表达式、日志 appender、Spring 注册文件以及字符串形式的类名和包名。

本轮以模块归属为主，保留现有业务实现。仍需后续独立重构的边界：`Bot` 同时负责会话和协议调用；`MessageProcessor` 在生命周期事件中协调联系人初始化；`CommonUtil`、`BilibiliService` 内部仍有多种职责；数据库日志 appender 使用系统日志持久化接口。目录归并不代表这些历史耦合已经完全消除，不应照此继续扩大跨模块依赖。

## 兼容性

- Java 全限定类名已经变化，外部 Java 扩展或自定义 Logback 配置需要使用新名称。完整文件映射见 `package-migration.tsv`。
- 新日志 key 为 `logging.level.com.haruhi.botserver`。启动时兼容旧 key `logging.level.com.haruhi.botServer`，按 Spring 属性源优先级选取，同一来源的新 key 优先。配置管理也能读取外部 YAML 中旧 key 的值，保存时使用新 key。
- 数据库表名、配置文件名、接口 URL 和功能权重未更改。
- WebUI 源目录 `views/wel` 改为 `views/dashboard`；已有 `/wel` URL 保持兼容。仍被路由引用的框架页面保留，演示首页移至 `webui/examples`；mock 入口仅开发构建加载。
- `instructions` 教学代码移至测试源集，不进入生产 JAR。

## 验证

要求 JDK 21、Maven 3.8+，前端使用能运行现有锁文件依赖的 Node/npm 环境。

```sh
mvn clean test
cd webui
npm ci
npm run build
cd ..
mvn package -DskipTests
```

后端无需外部网络的核心回归可单独运行：

```sh
mvn test -Dinternet-host=127.0.0.1 -Dtest=LegacyLoggingEnvironmentPostProcessorTest,MessageDispatcherPolicyTest,ConfigsTest,ConfigSpringIntegrationTest,BotTest,JmcomicSqliteServiceImplTest
```

`SearchEngineTest` 和 `JmcomicServiceTest.testDownload` 包含外部服务调用，完整测试需要对应网络与配置。首次迁移后执行 `clean`，避免旧包的 class 文件和旧配置残留。

本轮验证：上述 63 项核心测试通过，包含 17 项 Spring 集成测试；WebUI 生产构建通过（保留原有体积等构建警告）；Maven ZIP 打包通过。最终完整测试因外部服务访问需要额外授权而未重跑。独立代码审查未发现重要运行时回归。
