# haruhibot-server

基于 Java 21 / Spring Boot 的 QQ Bot，通过 NapCat 的反向 WebSocket 接收消息，提供可选 WebUI 管理界面。

## 开发与构建

后端需要 JDK 21 和 Maven 3.8+。请确认 JAVA_HOME 指向 JDK 21。

前端使用 Vue 2 / Vue CLI 5，先在 webui 目录执行 npm ci 安装锁定依赖，再执行 npm run build。

- Windows 完整构建：运行 build.bat。
- Linux/macOS 完整构建：运行 sh build.sh。
- 仅后端构建：build-back.bat 或 sh build-back.sh。
- 后端测试：mvn clean test。

构建逻辑在 scripts/，根目录脚本是兼容入口。构建脚本跳过测试，提交前请单独运行测试。仅后端构建会使用已有 webui/dist；首次构建或修改前端后请执行完整构建。

发布文件为 target/haruhibotServer.zip。解压后运行 start.bat 或 sh start.sh；运行脚本源文件在 scripts/runtime/。

WebUI 访问地址为 http://{ip}:{port}，账号与密码配置在 config/webui.properties。默认端口为 8090。

## NapCat 接入

安装并配置 [NapCat](https://napneko.github.io)，将反向 WebSocket 地址设置为 ws://{ip}:{port}/api/ws。Bot 作为服务端，NapCat 作为客户端。

## 项目结构

后端采用单 Maven 模块，以功能聚合代码，根包为 com.haruhi.botserver。B 站、聊天记录、图片搜索等功能的消息入口、业务服务和持久化代码均位于 features 下对应模块。

- [架构、包职责与兼容说明](docs/architecture.md)
- [配置管理说明](docs/config-redesign.md)
- [开发文档索引](docs/README.md)

结构迁移后首次构建请执行 mvn clean，避免旧包的 class 文件和配置残留。旧日志 key logging.level.com.haruhi.botServer 保持读取兼容，新配置使用 logging.level.com.haruhi.botserver。
