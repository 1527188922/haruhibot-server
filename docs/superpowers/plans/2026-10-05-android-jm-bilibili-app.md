# 安卓单机版「JM漫画 + B站视频」App 实施计划

> 本文档供**新会话**直接执行。新会话没有历史上下文，请先完整读完第 0～3 节再动手。

---

## 0. 给新会话的开场信息

**参考实现（只读，不要修改它）**

- 项目根目录：`D:\JavaProject\haruhibot-server`
- 技术栈：**Java 21** + Spring Boot（服务端）+ Vue2/Element-UI（Web 端）
  （`pom.xml` 里 `maven.compiler.source/target/release` 均为 `21`；本机 IDEA 项目 SDK 也是 21。跑黄金用例脚本时注意用 JDK 21）
- 当前分支：`dev/ai-refactor`
- 本任务只把它当**可执行的需求说明书 + 参考实现**：所有业务规则以它的源码为准。

**本次任务**

新建一个**纯单机安卓应用**（不用任何后端服务器），把下面两块功能完整搬到手机上：

1. **B站视频**：视频记录落库、视频下载到手机、列表展示与播放、删除。
2. **JM 漫画**：在线搜索、记录落库、下载（含图片解扰）、收藏夹、zip 导出、pdf 导出、阅读。

**必须遵守的已定决策（用户已确认）**

| 决策 | 内容 |
|---|---|
| UI | **完全使用安卓原生 UI 库**；具体框架（Compose / XML+View / 其他）由用户后续选型，本文档只写"能力要求" |
| 服务器 | **没有服务器**。所有数据、文件、数据库都在手机本地；不做多端同步 |
| 账号 | **不做 JM 账号/登录**（用户已确认暂不需要）。JM 请求只依赖时间戳签名，不依赖登录态（见 §5.1）；B站 Cookie 只在 B站模式内部配置，不做任何首次引导 |
| JM PDF | **必须实现**（不是可选项） |
| 导出密码 | 生成 zip/pdf 时弹**密码框**（默认填设置里的密码）+ **[确认]** / **[不设密码]** 两个按钮；`不设密码` = 不加密（见 §9 第 7 条） |
| 图片落盘 | `blockNum == 0` 时**直接落原图**，不再统一重编码成 webp |
| 下载任务 | **必须持久化**（杀进程后可恢复），进度粒度**到章节 + 具体图片**（见 §4.5） |
| 库导入 | **不做导入通道**（服务器上已有的库不搬，手机上重新添加/下载） |
| 语言 | **建议 Kotlin**（Compose 只能 Kotlin；JDK21 语法在安卓上不可用），最终由用户拍板（见 §12 语言选型） |
| B站清晰度 | 现阶段保持参考实现的行为（未设置 `fnval`，走 progressive `durl` 单文件直链），但**必须预留 DASH + MediaMuxer 合流的扩展点** |
| 参考实现 | 可自由重写、优化，但**行为要能对齐**（见第 12 节验收方式） |

**建议的目录**

```
<新 Android 工程>/
  docs/android-app-plan.md      ← 把本文件复制过去
  app/ ...
```

---

## 1. 范围：做什么 / 不做什么

### 1.1 要做（用户明确列出的）

| 功能 | 说明 |
|---|---|
| B站视频记录落库 | 视频元数据（BV/AV/CID、标题、封面、UP、标签、时长、简介等）存本地库 |
| B站视频下载 | 下载单文件 mp4 到手机本地；支持进度、取消、失败原因、重复下载拦截 |
| B站视频展示 | 列表、筛选、排序、分页、封面、下载状态、播放 |
| JM 下载 | 按本子/章节下载全部图片到本地，支持并发、续传（已存在的图片跳过）、进度、取消、重试 |
| JM 记录落库 | 本子、章节图片入库；"是否已入库/是否已下载"状态 |
| JM 收藏 | 收藏夹（文件夹）增删改、本子加入/移出收藏夹、按收藏夹浏览 |
| JM zip | 把本子目录打包成**带密码的 AES zip** |
| JM pdf | 把本子目录生成**带密码的 PDF**（每章书签目录） |
| JM 阅读 | 先进入**漫画详情页**（在线拉章节列表 + 标记已下载章节），点章节才进**阅读器**（翻页、缩放、章节切换） |

### 1.2 不要做（参考实现里有、但本 App 明确排除）

QQ/OneBot 相关的一切、B站直播订阅与推送（`t_bilibili_subscribe`）、WebSocket 推送层、登录鉴权与用户体系、配置管理页/日志监控/数据库管理/服务器文件管理等后台菜单、定时任务、词条/聊天记录/Pixiv 等其它功能模块。

另外**明确不做**：把服务器上已有的库（`jmcomic/`、`video/bilibili/` 目录和 `.db`）导入手机的通道 —— 手机上重新添加与下载即可（用户已确认）。

---

## 2. 参考实现地图（新会话必读）

### 2.1 必须精读的源文件

**JM 漫画**

| 文件（相对 `D:\JavaProject\haruhibot-server`） | 行数 | 作用 / 需要抄的逻辑 |
|---|---|---|
| `src/main/java/com/haruhi/botserver/features/jmcomic/service/JmcomicService.java` | 1622 | **核心**。JM 接口调用、签名、AES 解密、图片下载、解扰、zip、pdf、任务模型 |
| `.../features/jmcomic/service/JmcomicSqliteServiceImpl.java` | 1006 | 本子/章节图片/收藏夹的增删改查与分页查询语义 |
| `.../features/jmcomic/service/JmTaskQueue.java` | 454 | 任务队列：串行/并行、去重、取消、重试、进度上报 |
| `.../features/jmcomic/service/JmTaskContext.java` / `JmTaskPushService.java` | 小 | 任务上下文与进度回调（安卓上换成 Flow/回调） |
| `.../features/jmcomic/service/JmOnlineSearchHistoryStore.java` | 183 | 在线搜索历史（注意确认它的存储方式，见 2.4 待确认项） |
| `.../features/jmcomic/controller/JmcomicController.java` | 366 | 对外功能清单（哪些能力存在、参数是什么） |
| `.../features/jmcomic/client/model/*.java` | ~400 | `Album`/`Chapter`/`Series`/`SearchResp`/`UserProfile`/`DownloadParam` 数据模型 |
| `.../features/jmcomic/persistence/entity/*.java` | ~120 | 4 张表的字段定义 |
| `src/main/resources/config/jm.properties` | — | 需要变成 App 设置项的配置 |

关键行号锚点（`JmcomicService.java`）：

| 位置 | 内容 |
|---|---|
| 85～104 | JM API 密钥/版本/UA/超时重试常量 |
| 119～175 | `getJmApiDomain()`（域名与容错） |
| 177～200 | `login(username,password)`（JM 账号登录）——**本 App 不实现**，只作了解；主流程不依赖它 |
| 543～590 | **zip 打包**（zip4j + AES 密码） |
| 592～760 | **pdf 生成**（PDFBox：页面、书签、加密、权限） |
| 674～744 | `albumToPdf` / `addImageToPdf`（页面尺寸=图片像素尺寸，一章一个书签） |
| 746～790 | `sortFolders` / `sortFiles` / `extractChapterNumber` / `extractImageNumber`（章节与图片排序） |
| 798～930 | `downloadAlbum`（整本下载、章节循环、进度、重试） |
| 930～1020 | `downloadChapter`（并发分片、`.tmp` 中转、跳过已存在、防无限重试） |
| 1072～1085 | **`calculateBlockNum`（解扰块数算法）** |
| 1087～1164 | **`saveImg` / `stitchImg` / `copyImageBlock`（解扰实现，基于 `WritableRaster`）** |
| 1197～1260 | `requestAlbum`（`/album`，AES 解密响应） |
| 1253～1450 | `search`（`/search`）、`/chapter`、`/chapter_view_template` |
| 1439～1475 | `getScrambleId(chapterId)`（解析 `var scramble_id = `） |
| 1440～1520 | **`buildHeaders`（tokenparam/token 签名）+ `decryptData`（AES 解密）** |
| 1183～1189 | `buildAlbumFolderName`（本子文件夹命名规则） |

**B站视频**

| 文件 | 行数 | 作用 |
|---|---|---|
| `.../features/bilibili/service/BilibiliService.java` | 604 | B站接口：WBI 签名、视频详情、playurl、ticket、下载 |
| `.../features/bilibili/service/BilibiliVideoDownloadService.java` | 294 | 下载任务（内存任务表、`bvid_cid` 去重、`.downloading` 中转、快照） |
| `.../features/bilibili/service/BilibiliVideoService.java` | 218 | 视频列表/筛选/排序/分页、增删 |
| `.../features/bilibili/service/BilibiliVideoSqliteServiceImpl.java` | 153 | `t_bilibili_video` 的读写 |
| `.../features/bilibili/support/BilibiliIdConverter.java` / `BilibiliSidUtil.java` | 89+ | BV↔AV 互转、URL 解析出 id |
| `.../features/bilibili/client/model/bilibili/*.java` | ~400 | `VideoDetail`/`PlayUrlInfo`（只有 durl！）/`BilibiliBaseResp` |
| `.../features/bilibili/persistence/entity/BilibiliVideoSqlite.java` | ~105 | 表字段 |
| `src/main/resources/config/bilibili.properties` | — | cookie 等设置项 |

关键行号锚点（`BilibiliService.java`）：49（nav/WBI key）、94～120（`getVideoDetail`）、129～170（`getPlayUrlInfo`，注意 `fnval=0`、`fnval=1024` 被注释）、467～530（GenWebTicket）、532～640（WBI 签名与 mixinKey 缓存）。

### 2.2 前端（只作为 UI/交互参考，不移植代码）

`webui/src/views/jmcomic/*`（约 5200 行 Vue）与 `webui/src/views/bilibili-video/*`（约 2075 行 Vue）。**用途**：确定每个页面上有哪些按钮、字段、筛选条件、空态/错误态。UI 要原生重写，但功能点要对齐。

### 2.3 可以直接丢弃

`features/jmcomic/handler/JmcomicHandler.java`、`features/bilibili/handler/*`、`features/bilibili/job/BilibiliLiveJob.java`、`features/bilibili/service/BilibiliSubscribe*`、`*PushService.java`、`infrastructure/web/**`（拦截器/WebSocket）、`administration/**`、`configuration/**`（配置管理页）、`features/chatrecord|wordstrip|pixiv|searchimg` 等。合计约 1900+ 行无需移植。

### 2.4 待确认项（新会话动手前先确认）

1. `JmOnlineSearchHistoryStore` 的历史记录存在哪里（内存 / 文件 / 表）？—— 决定是否要在 Room 里加一张表。
2. 章节文件夹的**精确命名规则**（`extractChapterNumber(folderName.split("_")[0])` 提示是 `<序号>_<标题>`），以及图片文件名是否沿用 JM 原始文件名（`downloadChapter` 里 `chapterPath + File.separator + filename` 说明**沿用原始文件名**）。
3. `getScrambleId` 是否还有静态兜底表（只看到 `/chapter_view_template` 解析 + 异常日志，请通读 1439～1475 确认）。

---

## 3. 架构：模块划分与关键接口

### 3.1 模块建议

```
app/                     壳：开屏选择、导航、设置、下载中心、权限
core/
  network/               OkHttp 封装、UA/Referer、重试、代理设置
  database/              Room：Entities / Dao / Migration
  storage/               本地目录规划、SAF/MediaStore 导出、文件命名与清洗
  download/              下载引擎（Range 续传、并发、进度、取消、重试）
  taskqueue/             业务级任务队列（对齐 JmTaskQueue 语义）
  codec/                 ImageCodec（解码/解扰/编码）、ArchiveWriter（zip）、PdfWriter
feature-jm/              JM 接口客户端 + 仓库 + 用例
feature-bili/            B站接口客户端 + 仓库 + 用例
ui-jm/ ui-bili/ ui-common/
```

### 3.2 必须抽象出来的接口（这是移植成本和 DASH 扩展点的关键）

```kotlin
// 图片处理：把 JVM 的 ImageIO/AWT 换掉
interface ImageCodec {
    /** 读图片尺寸/格式（替代 ImageIO.read + ImageReader） */
    suspend fun readMeta(file: File): ImageMeta
    /** 解码 + 按块数解扰 + 重新编码（替代 saveImg/stitchImg） */
    suspend fun descrambleAndSave(srcTmp: File, dst: File, blockNum: Int, format: OutFormat)
    /** 转成 JPEG 字节（PDF 内嵌用：PDF 不支持 WebP） */
    suspend fun toJpeg(file: File, quality: Int): ByteArray
}

// 打包：zip（password == null/空 表示不加密，对应用户点"不设密码"）
interface ArchiveWriter { suspend fun zipDir(dir: File, out: File, password: CharArray?) }

// PDF：必须实现（章节书签 + 可选密码 + 权限；password == null/空 表示不加密）
interface PdfWriter { suspend fun albumToPdf(albumDir: File, out: File, password: String?) }

// 视频：现在是 durl 单文件；DASH 扩展点
sealed interface VideoStream {
    data class Progressive(val url: String) : VideoStream          // 现阶段的 durl
    data class Dash(val videoUrls: List<String>, val audioUrls: List<String>) : VideoStream // 预留
}
interface BiliStreamResolver { suspend fun resolve(bvid: String, avid: Long?, cid: Long): VideoStream }
/** 只被 Dash 分支使用；Progressive 分支直接落盘。预留实现：MediaMuxer */
interface StreamMuxer { suspend fun mux(video: File, audio: File, out: File) }
```

> **DASH 扩展点的硬性要求**：下载任务、进度、存储、播放全部只依赖 `VideoStream`/`BiliStreamResolver`，**不允许**在下载流程里硬编码 `getDurlFirst()`。将来只需新增一个 `DashResolver` + `MediaMuxerStreamMuxer`，其余代码不动。

### 3.3 并发/任务模型

参考实现的语义（要保留）：

- JM 任务队列 `JmTaskQueue`：任务 id、排队/执行中/成功/失败/取消、`jm.operation.parallel.enabled=false`（默认所有 JM 操作串行）、单本下载内按 `jm.download.threads` 分片并发、进度上报、"重试数不变则终止"的防死循环。
- B站下载：内存任务表，key = `bvid_cid`，同 key 只允许一个进行中任务；已完成任务只保留最近 20 条；**"是否已下载"始终以磁盘文件是否存在为准**（不是以任务状态为准）；下载中用 `<file>.downloading` 中转，完成才改名。
- 安卓侧必须这样做：`WorkManager`（可被系统调度、进程被杀后可恢复）+ 前台服务（长下载常驻通知）+ 协程 `Flow` 上报进度。**任务状态必须持久化**（用户已确认，参考实现只在内存里是它服务端场景的取舍，手机上不适用）。

---

## 4. 数据模型（Room）

直接对应当前 SQLite 表（表名常量见 `infrastructure/persistence/DataBaseConst.java`）。字段以下面为准（来自各 `*Sqlite` 实体类）。

### 4.1 `t_jm_album`（对应 `JmAlbumSqlite`，主键 `id` = JM 号，非自增）

`id: Long(PK)`、`name`、`albumFolderName`、`images`、`addTime`、`description`、`totalViews`、`likes`、`series`、`seriesId`、`commentTotal`、`author`、`tags`、`works`、`actors`、`relatedList`、`liked: Boolean?`、`isFavorite: Boolean?`、`collected: Boolean?`、`isAids: Boolean?`、`price`、`purchased`、`raw`、`createTime`、`modifyTime`

### 4.2 `t_jm_chapter_image`（对应 `JmChapterImageSqlite`）

`id: Long(PK,自增)`、`albumId`、`chapterId`、`chapterSort`、`chapterTitle`、`chapterName`、`chapterAddTime`、`seriesId`、`liked: Boolean?`、`isFavorite: Boolean?`、`imageFile`、`imageSort`

> 参考实现对该表有专门的自定义查询（`src/main/resources/mapper/features/jmcomic/JmChapterImageSqliteMapper.xml`：`selectChapterList`、`selectImages` 等），移植时照抄 SQL 语义。

### 4.3 `t_jm_favorite`（收藏夹）/ `t_jm_favorite_album`（关联）

- `t_jm_favorite`：`id`、`name`（**唯一**）、`sortOrder`、`createTime`、`modifyTime`
- `t_jm_favorite_album`：`id`、`favoriteId`、`albumId`、`createTime`；唯一索引 `(favorite_id, album_id)`
- 默认收藏夹名：`默认收藏夹`

### 4.4 `t_bilibili_video`（对应 `BilibiliVideoSqlite`）

`id: Long(PK,自增)`、`bvid`、`avid: Long?`、`cid: Long?`、`title`、`tag`、`desc`（列名是保留字，参考实现用 `` `desc` ``）、`pic`、`duration: Long?`、`ownerMid: Long?`、`ownerName`、`ownerFace`、`videoDetailRaw`、`playUrlRaw`、`pubdate: Long?`、`ctime: Long?`、`createTime`、`updateTime`
唯一索引：`(bvid, cid)`

### 4.5 安卓侧新增：持久化下载任务（**已确认必须做**，进度到章节 + 具体图片）

参考实现的任务只在内存（重启即清空），手机上必须落库，否则杀进程后进度全丢。建议两张表：

**`t_download_task`**（任务主表）

| 字段 | 说明 |
|---|---|
| `id` | 自增主键 |
| `type` | `JM_ALBUM` / `BILI_VIDEO` |
| `bizKey` | JM：aid；B站：`bvid_cid`（**同 bizKey 只允许一个进行中任务**，照抄参考实现的去重规则） |
| `status` | `QUEUED` / `RUNNING` / `PAUSED` / `SUCCESS` / `FAIL` / `CANCELLED` |
| `totalChapters` / `doneChapters` | 章节级进度 |
| `totalImages` / `doneImages` | 图片级进度（B站视频用 `totalBytes` / `downloadedBytes` 表达） |
| `stageText` | 当前阶段文案（如"下载漫画图片"），对应参考实现的进度阶段上报 |
| `message` | 失败原因 |
| `payload` | JSON：本次任务的参数快照（aid、章节 id 列表等），用于恢复时重建上下文 |
| `createTime` / `startTime` / `endTime` / `updateTime` | 时间戳 |
| `retryCount` | 重试次数（配合参考实现的指数退避） |

**`t_download_task_chapter`**（JM 专用章节明细，用于"进度到章节"）

`id`、`taskId`、`chapterId`、`chapterTitle`、`sort`、`status`、`totalImages`、`doneImages`、`message`、`updateTime`
唯一索引 `(taskId, chapterId)`

**"具体图片"粒度怎么实现（重要设计取舍）**

- **不建"一张图一行"的表**：一本几百章 × 每章几十张图，会写出几万行记录，收益极低。
- **每章的 `doneImages/totalImages` 计数器 + 磁盘文件为准**：
  - 断点续传的判定 = 「该图片文件是否已存在于本地」——与参考实现 `downloadChapter` 里 `.filter(e -> !e.getImgFile().exists())` 的行为完全一致；
  - 页面上"第 3 章 12/45"这种图片级进度 = 该章的计数器（恢复时按磁盘重算，照抄参考实现 `reporter.chapterImages(total, 已存在数)` 的做法，保证进度不回退、不重复累加）。
- 如果将来确实要"单张图的下载状态/失败原因"，再加一张 `t_download_task_image` 只记录**失败/待重试**的图片（稀疏表），不要记录成功的。
- 图片级粒度的断点续传验收：下载到一半杀进程 → 重启后继续，已下好的图片**不重新下载**（可用网络流量或文件 mtime 验证）。

- `t_settings` 或 DataStore：见附录 B 的配置项。

---

## 5. 网络契约

### 5.1 JM 漫画接口

- **域名**：`jm.api_domain`（默认 `www.cdnbea.net`，见 `jm.properties`）；图片域名 `IMAGE_DOMAIN = cdn-msp2.jmapiproxy2.cc`（`JmcomicService` 第 89 行）。都必须做成可配置 + 可换域名。
- **UA**：固定 Chrome UA（第 91 行），必须带上。
- **签名（每次请求都要）**：`ts = 当前时间戳`；`tokenparam = "$ts,2.0.13"`；`token = md5(ts + secret)`。header 名是 `token`、`tokenparam`、`user-agent`（见 `headerParam()`，第 1485～1494 行）；`secret` 有两个：`18comicAPP`（第 1488 行，普通接口）与 `18comicAPPContent`（第 1444 行，内容类接口，如 `/chapter_view_template`）。具体哪个接口用哪个 secret **以 1440～1520 行为准**。
  **注意：签名只依赖时间戳，不依赖登录态**，所以本 App 不需要 JM 账号。
- **响应解密**：响应 JSON 的 `data` 字段是密文；`key = md5(ts + "185Hcomic3PAPP7R")` 得到的 **32 字符 hex 串，取其 ASCII 字节（32 字节）当 AES 密钥**——即 AES-256，**不是 md5 摘要的前 16 字节**（第 1499～1504 行，写错会导致解密失败）；`AES/ECB/PKCS5Padding` 解密（第 1496～1520 行）。
- **接口清单**：`POST/GET https://{domain}/album`、`/search`、`/chapter`、`/chapter_view_template`（第 1200、1264、1423、1441 行）。`/login`（第 178 行）**本 App 不实现**。参数与响应结构以源码为准，响应模型对应 `client/model/*`。
- **图片 URL 规则**（第 1479、1482 行）：封面 `https://{cdn}/media/albums/{aid}.jpg`；章节图 `https://{cdn}/media/photos/{chapterId}/{filename}`。
- **重试**：`jm.request.album/search/chapter.retry` 是重试次数，指数退避（1s/2s/4s…），重试期间**沿用第一次请求的签名时间戳**（`jm.properties` 注释里写明了，照抄）。
- JM 模式设置里要暴露：**备选 API 域名**（不需要账号密码）、下载线程数、是否并行、zip/pdf 密码、本子名长度上限、各接口重试次数。

### 5.2 B站接口

- **接口**：
  - `GET https://api.bilibili.com/x/web-interface/nav` —— 取 WBI 的 `img_key`/`sub_key`（第 49、616 行）
  - `GET https://api.bilibili.com/x/web-interface/wbi/view/detail` —— 视频详情（第 102 行）
  - `GET https://api.bilibili.com/x/player/wbi/playurl` —— 播放地址（第 151 行）；请求带 `fnver=0`、**没有设置 `fnval`**（`fnval=1024` 那行在源码里被注释掉了，见第 142、149 行），所以返回的是 progressive 的 `durl`；下载只取 `durl` 第一个链接（`PlayUrlInfo` 里也只建模了 `durl`，没有 DASH 字段）
  - `POST https://api.bilibili.com/bapis/bilibili.api.ticket.v1.Ticket/GenWebTicket` —— 风控 ticket（第 467 行）
- **WBI 签名**：`img_key+sub_key` → 按固定混淆表重排 → `mixinKey`（缓存，第 589～602 行）；请求参数加 `wts` 后按 key 排序拼 query，`w_rid = md5(query + mixinKey)`。**必须逐行对齐**第 532～640 行。
- **Cookie**：`SESSDATA` + `bili_jct`（`bilibili.properties`），另有自动获取的 `ticket`；请求头需带 `Referer: https://www.bilibili.com/`（第 64 行）。**这些放在「B站模式的设置页」里让用户填写**（不做首次引导，见 §9）。
- **BV↔AV 与 URL 解析**：`BilibiliIdConverter`、`BilibiliSidUtil`；应用内要支持粘贴完整 bilibili 链接（含 `b23.tv` 短链？参考实现的解析正则见 `BilibiliService` 第 78 行，需确认短链是否需要跟随跳转）。
- **下载**：直接把 durl 的 URL 落地为 `.mp4`（`BilibiliVideoDownloadService` 第 174～219 行）。
- **视频时长限制**：`bilibili.download_video.duration_limit`（默认 600 秒）——照抄为设置项。

---

## 6. 算法移植清单（逐条：源位置 → 安卓实现要点）

| # | 算法 | 源位置 | 安卓实现要点 |
|---|---|---|---|
| A1 | JM 请求签名 `tokenparam/token` | JmcomicService 1440～1520 | 纯 MD5，`MessageDigest`，无坑 |
| A2 | JM 响应 AES/ECB 解密 | 1496～1520 | `javax.crypto.Cipher` 安卓可直接用；注意 key = `md5(ts+secret)` 的**前 16 字节** |
| A3 | 解扰块数 `calculateBlockNum` | 1072～1085 | 规则：`chapterId < scrambleId → 0`；`< 268850 → 10`；否则 `x = chapterId<421926 ? 10 : 8`，`blockNum = (md5(chapterId+filename) 末位字符转 int) % x * 2 + 2`。**注意 `char % x` 用的是字符码点取模**，别改成 hex 解析 |
| A4 | scrambleId 获取 | 1439～1475 | `GET /chapter_view_template`，正则取 `var scramble_id = `；异常/取不到时的兜底行为要照抄 |
| A5 | 图片解扰 `stitchImg` | 1103～1164 | 语义：把图**按垂直方向分成 blockNum 块**，第 0 块吸收 `height % blockNum` 的余数，然后**块顺序倒置**写回。安卓建议：`Bitmap` + `Canvas.drawBitmap(src, srcRect, dstRect, paint)` 逐块拷贝（比逐像素快几个数量级）；也可 `copyPixelsToBuffer` 整行搬运 |
| A6 | 图片落盘 `saveImg` | 1087～1101 | 参考实现**无论是否解扰都重新编码成 webp**；**本 App 已确认改为：`blockNum == 0` 时直接把原文件落盘（不重编码）**，只有 `blockNum > 0` 才 解码→解扰→编码（安卓用 `Bitmap.compress`，格式建议 WEBP_LOSSLESS；若要进一步省 CPU/流量也可保持原格式）。注意：非 webp 原图（如 jpg）本来就不解扰，直接落盘即可 |
| A7 | 只对 webp 解扰 | `downloadChapter` 948～954 | `ext` 不是 `webp` 时 `blockNum=0`，照抄 |
| A8 | zip 打包（AES） | 543～590 | 参考用 `zip4j` + `EncryptionMethod.AES`，密码取 `jm.password.zip`。安卓可直接用 `net.lingala.zip4j:zip4j`（纯 Java）；**密码来自导出密码框**（默认预填配置值），用户选 **[不设密码]** 或留空时**不加密**（`ZipFile` 不传密码）；若不想引第三方库也可自己用 `javax.crypto` 写 AES-ZIP（工作量大，不推荐） |
| A9 | **PDF 生成** | 592～760 | 见第 7 节（本任务最大难点） |
| A10 | 本子文件夹命名 | 1183～1189 | `<清洗后的标题>_JM<aid>`；按**字节数**截断且按字符边界，非法字符 `\ / : * ? " < > |` 与控制字符替换为 `-`；安卓侧改用应用私有目录，风险下降但仍建议保留清洗逻辑（导出到外部存储时 Windows 兼容名更安全） |
| A11 | 章节/图片排序 | 746～790 | 从名字里抽数字排序（`提取序号`），照抄；注意名里没数字时的兜底 |
| A12 | B站 WBI 签名 | BilibiliService 532～640 | `HmacSHA256`/`MD5` 均可用；mixinKey 要缓存并有过期时间 |
| A13 | BV↔AV 转换 | `BilibiliIdConverter` | 位运算 + 固定字符表，纯逻辑，照抄 |
| A14 | 文件名清洗 `sanitizeFileName` | `shared/util/FileUtil.java` 197 行起 | 移植；安卓上还要额外处理 SAF/MediaStore 的显示名 |

---

## 7. JM PDF 生成（本任务最大难点，单独说明）

**参考实现的行为（必须对齐）**：

1. 每张图片一页，页面尺寸 = **图片像素尺寸**（`new PDRectangle(image.getWidth(), image.getHeight())`）。
2. 图片按章节文件夹排序；每个章节生成一个**书签**（outline），指向该章第一页；根书签标题 `目录`；`PageMode = USE_OUTLINES`。
3. **加密**：`StandardProtectionPolicy(password, password, permission)`，`encryptionKeyLength = 128`；权限：`canModify=false`、`canExtractContent=false`、`canExtractForAccessibility=false`。密码来自**导出时的密码框**（默认预填 `jm.password.pdf`）；用户点 **[不设密码]** 或留空时**不加任何保护**（不调用 `protect()`）。
4. 图片用 `LosslessFactory` 内嵌（无损 = Flate 压缩），文件会明显偏大。

**安卓实现路径（必须先在 P0 做技术验证）**：

| 方案 | 说明 | 风险 |
|---|---|---|
| **B1. `com.tom-roush:pdfbox-android`** | PDFBox 的安卓移植，`LosslessFactory.createFromImage(Bitmap)`；有 `StandardProtectionPolicy` | 需验证：**AES-128 加密 + 书签 + `PDPageContentStream`** 三件套在安卓上是否都可用；版本较老（基于 PDFBox 2.0.x），大页面可能有兼容问题 |
| **B2. 手写 PDF 写入器** | 自己拼 PDF 对象：页树、每页一个 XObject（JPEG 走 `DCTDecode` 直接内嵌，无需重新编码）、`Outlines` 目录树、标准安全字典（RC4-128 或 AES-128，用 `javax.crypto`） | 工作量大（约 400～700 行）、要自己保证 PDF 结构正确（用 Adobe/Chrome/系统 PDF 阅读器验收）；但**体积和速度都优于 B1**，且不引第三方库 |
| B3. 其他 PDF 库 | 例如 iText（AGPL，商用需授权）、pdfium（只能渲染不能生成） | 授权/能力不满足 |

**建议**：P0 先花 0.5～1 天做 B1 的 spike（生成一个带密码 + 书签的 PDF，用系统阅读器和 Chrome 打开验证）。**如果 B1 的加密/书签任一不满足，就走 B2**，并在实现时用"DCTDecode 直嵌 JPEG"替代 `LosslessFactory`（体积可以从几百 MB 降到几十 MB）。

**其它约束**：

- PDF 单页尺寸上限 14400×14400（PDF 规范），JM 长图页高接近上限时要注意；同时很多阅读器对超高页面体验很差 —— 参考实现就是这样，先对齐；如需优化（例如按高度切片）属于**行为差异**，要单独确认。
- WebP 不能直接内嵌 PDF，必须转 JPEG（`Bitmap.compress(JPEG)`）；有损转换会掉画质，考虑质量 90+。
- 生成大 PDF 时必须**流式写**（先写文件，不要在内存里拼 ByteArray），并放在前台服务里跑（可能几分钟）。

---

## 8. Android 平台必须做的事

| 项 | 要点 |
|---|---|
| 存储 | 库文件放应用私有目录（`filesDir`/`getExternalFilesDir`），避免分区存储限制；**导出** zip/pdf 走 MediaStore（Downloads）或 SAF（`ACTION_CREATE_DOCUMENT`）；JM 库可能几 GB，要提供"占用统计 + 清理" |
| 下载 | 长下载用**前台服务 + 常驻通知**（进度、暂停/取消）；用 WorkManager 做可延后/可恢复的队列；请求忽略电池优化；Wi-Fi/移动网络策略让用户选 |
| 续传 | 图片按"文件是否存在"跳过（照抄参考实现）；单文件 mp4 用 HTTP `Range` 续传（参考实现的 `BilibiliVideoStreamController` 已证明服务端支持 Range，但那是对服务端；**对 B站 CDN 是否支持 Range 需要实测**，不支持就整文件重下） |
| 播放 | Media3/ExoPlayer，播放本地 mp4；全屏、屏幕常亮、后台播放按需；**预留 DASH 分支**（Media3 也支持 DASH，但我们的 DASH 是要落盘，不是在线播） |
| 阅读器 | 原生图片阅读器：翻页、双指缩放、双击缩放、左右/上下翻页模式、章节跳转、预加载相邻页；注意**超长图 + ARGB_8888 会 OOM**（400×12000 ≈ 18MB），建议 RGB_565 + 串行处理 + 及时 `recycle()`，必要时用 `BitmapRegionDecoder` 分块 |
| 网络 | 统一 UA/Referer；JM 与 B站都需要**可配置代理**（JM 在部分网络下需要）；HTTPS 证书异常、超时重试（照抄参考实现的指数退避） |
| 权限 | 通知（Android 13+ 运行时权限）、前台服务（`FOREGROUND_SERVICE_DATA_SYNC`）、网络；导出到相册才需要媒体权限 |
| 其它 | 返回键、深链（分享 bilibili 链接给 App）、崩溃日志本地留存、备份/恢复（可选：导出数据库 + 库目录） |

---

## 9. 页面清单（原生 UI 要实现的界面）

**壳**

1. **开屏选择页**：两个大卡片（JM漫画 / BILIBILI视频）；可"记住上次选择"（下次直接进对应模式，仍保留切换入口）。
   **不做任何首次引导/向导**：不填 JM 账号（不需要），不填 B站 Cookie（在 B站模式的设置里配）。
2. **主框架**：两个模式各自独立的导航；顶栏/底栏要有**切换到另一模式**的入口；各自有设置入口（配置按模式分开，互不干扰）。
3. **设置（分三处）**：
   - **B站模式设置**：`SESSDATA`、`bili_jct`（可提供"粘贴整段 Cookie 自动解析"）、时长上限、清晰度（现阶段锁定为现状，仅展示）、ticket 状态。
   - **JM 模式设置**：API 域名（可多备选）、下载线程数、是否允许不同 JM ID 并行、zip 密码、pdf 密码、本子名长度上限、各接口重试次数。
   - **通用设置**：存储目录与占用统计/清理、网络（代理、超时）、通知与后台（前台服务、电池优化引导）。

**JM 模式**

4. **本子库**（对应"JM主记录"）：搜索/筛选（ID、名称、作者、标签、是否收藏）、排序、分页、卡片或列表、批量操作（删除记录、批量下载、加入收藏夹、生成 zip/pdf、删除本地文件）、单条操作（**点卡片进详情页**、下载、收藏）。
5. **在线搜索**：JM 在线搜索（关键字、排序方式、分页）、搜索历史（新增/查看/删除）、"入库"/"入库并下载"、**点结果进详情页**。
6. **收藏夹**：收藏夹列表（增删改、排序）、收藏夹内本子列表、批量加入/移出、**点本子进详情页**。
7. **漫画详情页（本模式的核心中转页，替代原来的"章节信息"页）**：
   - 进入方式：本子库 / 在线搜索 / 收藏夹里点一本漫画。
   - 顶部信息：封面、标题、JM 号、作者、标签、页数/章节数、收藏状态、本地占用大小。
   - 章节列表：**在线请求该本子的章节列表**。数据来源很直接：`GET /album` 的响应里 `Album.series` 就是**整本的章节列表**（`Series.id` 即 `chapterId`，另有 `name`/`title`/`sort`），不需要逐章再请求；某章的图片文件名列表来自 `GET /chapter` 的 `Chapter.images`（或直接读本地库）。
     每一章显示标题、图片数、**"已下载 / 部分下载 / 未下载"标记**——判断依据是本地 `t_jm_chapter_image` 记录 + 磁盘上图片文件是否存在（参考实现的"是否已下载"一向以磁盘文件为准）。
   - 章节级操作：整本下载、下载/重下某一章、删除某一章的本地图片（放长按菜单或行内按钮）。
   - 本子级操作：加入/移出收藏夹、**生成 zip / 生成 pdf（点这两个按钮都要先弹密码框，见下方"导出密码框"）**、删除本地文件、删除记录。
   - **导出密码框（已确认的交互）**：点"生成 zip"/"生成 pdf" → 弹框：
     - 一个密码输入框，**默认预填设置里的密码**（zip 用 `jm.password.zip`、pdf 用 `jm.password.pdf`）；
     - **[确认]** → 用输入框里的密码生成；**[不设密码]** → 生成**不加密**的 zip/pdf（不再二次确认，但结果要在导出完成提示里写明"未加密"）；
     - 输入框内容要支持显示/隐藏切换（密文），并可留空（留空等同不设密码）；
     - 生成中要显示进度（大 PDF 可能几分钟），放在前台服务里跑，见 §8。
   - **点某一章 → 进入阅读器**。
8. **阅读器**：从详情页的章节进入；本地图片翻页阅读、双指缩放、章节内跳页、**上一章/下一章切换**（不提供"凭空按 ID 检索章节"的独立页面）；图片缺失/未下载时给出明确提示与一键下载。
9. **任务中心**：进行中/已完成任务（进度、阶段文案、取消、重试、清空已完成）——对应 `jm-task-panel.vue`。

**B站模式**

10. **视频列表**：搜索/筛选（UP、标签、是否已下载、是否入库）、排序、分页、卡片（封面/标题/UP/时长/下载状态）、批量删除。
11. **添加视频**：粘贴链接/BV/AV → 抓详情 → 入库（对应 `BilibiliVideoController.add/refresh`）。
12. **下载中心**：进行中（进度）、最近完成（最多 20 条）、失败原因、重试、删除文件。
13. **播放页**：本地 mp4 播放；未下载时提示下载；播放进度记忆（可选）。

---

## 10. 分阶段实施计划

> 每阶段结束必须有**可运行的产物 + 验收记录**，不要一次性写完再测。

### P0 环境与基线（2～3 天）

- 建空工程、模块划分、DI、日志、CI（lint + unit test）、把本文件复制进工程。
- **spike 1**：JM 签名 + AES 解密（用源项目产出的黄金数据对比，见第 11 节）。
- **spike 2**：PDF 方案选型（B1 验证加密+书签；不通过就定 B2）。
- **spike 3**：解扰在 Android 上跑通（取一张真实 webp，Java 版与 Kotlin 版输出逐像素对比）。
- 验收：三个 spike 都有结论记录（写在工程 docs 里）。

### P1 基础设施（4～6 天）

- Room 建 5 张业务表 + **`t_download_task` / `t_download_task_chapter`（持久化任务，见 §4.5）** + 迁移方案；网络层；设置存储；目录规划；下载引擎（Range 续传 + 并发 + 取消）；前台服务 + 通知。
- 验收：能下载一个任意大文件到私有目录，断网/杀进程后能续传，通知显示进度；**杀掉进程再启动，任务列表和进度能从数据库恢复（RUNNING 任务按磁盘现状重算进度后继续）**。

### P2 JM 数据与浏览（4～6 天）

- JM 客户端（**搜索/本子详情/章节列表/章节图片列表**，不含登录）、本子与章节入库、收藏夹 CRUD、`本子库 / 在线搜索 / 收藏夹 / 漫画详情` 4 个页面（详情页要能在线拉章节并标记本地已下载的章节）。
- 验收：搜到本子 → 入库 → 进详情页看到章节列表（已下载章节有标记）→ 收藏 → 重启 App 数据仍在；与参考实现同一 aid 的入库字段一致。

### P3 JM 下载（6～8 天）

- 章节图片下载（并发、`.tmp` 中转、跳过已存在、防死循环重试）、解扰落盘（**`blockNum==0` 直接落原图**）、**章节级 + 图片级进度持久化**、取消、重试、任务中心页面。
- 验收：
  - 下载一整本，图片数量/命名/内容与参考实现一致（抽样逐像素比对解扰结果）；
  - 下载中取消能停下；已下载的重复下载会跳过；
  - **下载到一半杀进程 → 重启后从断点继续，已完成图片不重下**；进度显示能精确到"第 N 章 x/y 张"。
  - `blockNum==0` 的图片文件与源文件字节一致（未被重新编码）。

### P4 JM 导出 zip / pdf（4～6 天，PDF 占大头）

- zip（AES 密码 / 不加密两种）；pdf（密码 / 不加密、书签、权限、页尺寸）；**导出前的密码框交互（默认填配置密码 + [确认] / [不设密码]）**。
- 验收：zip 用密码能解压、错误密码打不开、选"不设密码"时无密码可直接解压；PDF 用错误密码打不开、用正确密码能打开、书签按章节、权限（不可修改/不可复制）符合预期、选"不设密码"时无密码可打开；导出到 Downloads 能在系统文件管理器看到。

### P5 B站（4～6 天）

- 详情入库（WBI 签名）、列表/筛选/排序、下载（`VideoStream.Progressive` 分支）、播放（Media3）、删除文件与记录、下载中心。
- 验收：粘贴一个链接 → 入库 → 下载 → 播放；同 `bvid+cid` 不能重复下载；删除文件后状态回到"未下载"；**`BiliStreamResolver` 换成 Dash 实现时其余代码不改动**（用一个假的 Dash resolver 跑一遍单测证明扩展点成立）。

### P6 壳与体验（3～4 天）

- 开屏选择（不做首次引导）、模式切换、分模式设置页 + 通用设置页、**漫画详情页与阅读器的衔接打磨**、占用统计与清理、错误提示与空态。
- 验收：从开屏选 JM → 用 → 切到 B站 → 用 → 返回 JM 状态保留；各页面无 ANR/崩溃。

### P7 验收与真机（3～5 天）

- 真机（低端机 + 高端机各一台）跑第 12 节验收清单；内存/流量/电量粗测；长时间下载稳定性。
- 交付：可安装 APK + 验收记录 + 已知问题清单。

**总量**：单人全职约 **6～8 周**（比上一版 +约 1 周：导出密码框、任务持久化与章节/图片级进度都从"可选"变成了"必须"；UI 框架若选型不熟或 PDF 走 B2，时间继续上浮）

---

## 11. 测试策略

### 11.1 纯逻辑黄金用例（最高性价比）

源项目是普通 Maven 工程（**JDK 21**），这些算法都是纯函数，**在源项目里写一个临时 `main`/JUnit 打印期望值**即可（不需要联网）。
现成入口：`JmcomicService` 末尾（第 1522 行起）本来就有一个 `main(...)` + 若干 `testN()` 的临时测试方法，可以直接照这个套路加打印、跑 `mvn -q exec:java` 或 IDE 里直接运行。

| 用例 | 输入 | 期望输出 |
|---|---|---|
| `calculateBlockNum` | 若干 `(scrambleId, chapterId, filename)` | 用 Java 版打印，Kotlin 版断言一致（覆盖 268850 / 421926 两个阈值边界） |
| JM 签名 | 固定 `ts` | `token`、`tokenparam` 字符串完全一致 |
| AES 解密 | 固定 `ts` + 用同一密钥加密的密文 | 明文一致（**注意是 AES-256：32 字符 hex 串的 ASCII 字节做密钥**） |
| WBI 签名 | 固定 `img_key/sub_key/参数表/wts` | `w_rid` 一致 |
| BV↔AV | 已知 `BV1xx411c7mD` 等 | 与 Java 版一致 |
| 文件名清洗 | 含非法字符/超长/中文的标题 | 与 Java 版逐字节一致 |
| 解扰 | 一张真实 webp + 指定 blockNum | 与 Java 版输出**逐像素一致**（把 Java 版输出存成基准图） |

### 11.2 其它

- **网络契约**：MockWebServer 回放真实响应样本（把参考实现抓到的 JSON 存成 fixture），验证解析、AES 解密、分页字段。
- **数据库**：Room 迁移测试 + Dao 语义测试（分页/排序/唯一索引冲突）。
- **下载引擎**：本地起一个 HTTP 服务模拟 Range 分片、超时、断流。
- **UI**：关键路径的 instrumented test（开屏选择 → 列表 → 详情页 → 下载 → 阅读/播放），外加**导出密码框**三种路径（默认密码确认 / 改密码 / 不设密码）与**杀进程恢复**（下载中断后重启继续）。
- **持久化任务**：Dao 测试（同 `bizKey` 去重、RUNNING 任务恢复时按磁盘重算进度、章节计数器不回退）。
- **真机验收清单**（P7 逐条打勾）：见第 10 节各阶段"验收"，外加：弱网、切后台、来电打断、杀进程恢复、存储写满、超长图阅读、大 PDF 生成、密码错误提示、代理可用。

---

## 12. 风险与开放问题

**技术风险**

1. **PDF 是最大不确定项**：P0 spike 必须先验证 B1（PdfBox-Android）的加密+书签能力，否则切 B2（手写）。这会影响 1～2 周排期。
2. **超长图内存**：JM 图片很长，解码 + 解扰 + 编码需要整图在内存；必须 RGB_565 + 串行 + 及时回收，否则中低端机 OOM。
3. **JM 接口是逆向的 App API**：密钥/版本/域名随时可能变（参考实现把域名和版本都做成了配置），App 侧也要**把域名/版本/secret 留在可远程更新的位置**（例如设置项 + 内置多套备选）。
4. **B站 cookie/WBI/风控**：需要用户自己提供 cookie；`GenWebTicket` 是风控相关，行为可能变；durl 清晰度受限（用户已确认现阶段接受）。
5. **B站 CDN 是否支持 Range**：影响 mp4 断点续传，需实测；不支持就整文件重下。
6. **durl 兼容性**：部分视频可能只返回 DASH（`durl` 为空），此时需给出明确错误提示并依赖后续 DASH 实现。
7. **后台被杀**：国产 ROM 对前台服务/WorkManager 限制严格，需要引导用户加白名单。

**合规/分发风险（需用户知情）**

- JM 属成人内容，Google Play 不可能上架，实际是自签名侧载；参考实现里的逆向 secret 打进 APK 等于公开（可反编译）。
- 保留"仅个人使用"的范围，不要做分享/传播功能。

**需要用户决策的开放问题 → 已全部拍板（2026-10-05 补充）**

1. ~~zip/pdf 密码策略~~ → **已确认**：生成时弹**密码输入框**，输入框**默认填入设置里的密码**（`jm.password.zip` / `jm.password.pdf`），弹框提供 **[确认]** 与 **[不设密码]** 两个按钮（`不设密码` = 不加密）。实现要求见 §9 第 7 条。
2. ~~图片是否统一重编码~~ → **已确认**：`blockNum == 0` 时**直接落原图**（不重新编码），只有需要解扰时才解码→解扰→编码。
3. ~~下载任务是否持久化~~ → **已确认**：**必须持久化**（杀进程后可恢复），**进度粒度要到"章节 + 具体图片"**。实现要求见 §4.5、§10 P1/P3。
4. ~~是否需要库导入通道~~ → **已确认**：**不做导入通道**，手机上重新添加/下载即可。

**仍待用户拍板（不阻塞文档，但开工前必须定）**

5. **UI 框架 + 开发语言**：UI 框架由用户后续选型；语言**建议 Kotlin**（理由见下方"语言选型"）。这两项定了才能建工程。
6. PDF 的页面尺寸：参考实现是"一张图一页、页面尺寸=图片像素尺寸"（JM 长图会出现 800×12000 这种超高页面，部分阅读器体验差）。**默认先照抄对齐**；是否改成按高度切片，等 P4 做完再评估（属于行为差异，需用户确认）。

### 语言选型：Kotlin 还是 Java？（用户提问，给出结论）

**结论：用 Kotlin。** 理由（针对本任务）：

1. **如果要上 Compose，Java 根本用不了**——Compose 是 Kotlin 编译器插件，只能在 Kotlin 里写（Java 只能从 Kotlin 侧互操作调用）。UI 虽然是"待选型"，但候选人里 Compose 是主流，选 Java 等于把这条路堵死，只能写 XML+View。
2. **Java 21 的语法在 Android 上不可用**：参考实现是 JDK 21（`pom.xml` 里 release=21），里面用到了 record（例如 `BilibiliVideoStreamController.ByteRange`）等新语法；Android 的 Java 语言级别最高只到 Java 11/17 的一小部分（靠 desugaring），record/sealed 这些**照抄不了**。也就是说"用 Java 移植"并不能真的复制粘贴，照样得改写。
3. **协程/Flow 与参考实现的模型天然对应**：`JmTaskQueue` + `CompletableFuture` + 进度回调 → Kotlin 的 `coroutine + Flow`；`WorkManager`/`Room`/`Media3` 全部是 Kotlin-first（Room 的 DAO、KSP 注解处理、Flow 查询）。
4. **空安全**直接消掉一大类崩溃（本项目大量"字段可空/默认值"的语义，正好用 `?` 和 sealed class 表达）。
5. **生态与官方立场**：Google 自 2019 起是 Kotlin-first，新 API/示例/文档默认 Kotlin；Kotlin 与 Java 可互操作，不存在"用不了 Java 库"的问题（`javax.crypto`、`zip4j` 这些照用）。

**什么情况下才选 Java**：只有"改 XML+View、团队完全不会 Kotlin、且不接受学习成本"这一种。即便如此仍**不建议**（收益仅是少学一门语言，代价是失去 Compose + 协程 + 全部 Kotlin-first 生态）。也不建议 Java/Kotlin 混写。

> 如果用户选 Kotlin：本文档里所有 `xxx.kt` 的映射、`sealed interface`、`suspend fun`、`Flow` 写法都可直接执行。
> 如果最终选 Java（XML+View）：§3.2 的接口用 Java 表达（`VideoStream` 用抽象类+子类代替 sealed interface），其余章节（数据模型、算法、网络契约、阶段计划）完全不受影响。

---

## 附录 A：源文件 → 目标类映射（建议）

| 源（Java） | 目标（Kotlin） |
|---|---|
| `JmcomicService`（接口+签名+解密） | `feature-jm/.../JmApiClient.kt` |
| `JmcomicService`（下载/解扰/zip/pdf） | `feature-jm/.../JmDownloader.kt` + `core/codec/ImageCodec.kt` + `ArchiveWriter.kt` + `PdfWriter.kt` |
| `JmTaskQueue` + `JmTaskContext` + `JmTaskPushService` | `core/taskqueue/JmTaskQueue.kt`（+ Flow 进度） |
| `JmcomicSqliteServiceImpl` + 4 个 entity + mapper xml | `core/database/...`（Room Entity/Dao/Repository） |
| `BilibiliService` | `feature-bili/.../BiliApiClient.kt`（WBI、详情、playurl、ticket） |
| `BilibiliVideoDownloadService` | `feature-bili/.../BiliDownloader.kt` + `core/download/DownloadEngine.kt` |
| `BilibiliVideoService` + `BilibiliVideoSqliteServiceImpl` | `feature-bili/.../BiliVideoRepository.kt` + Room |
| `BilibiliIdConverter` / `BilibiliSidUtil` | `feature-bili/.../BiliId.kt` |
| `shared/util/FileUtil`（文件名/目录相关） | `core/storage/FileNaming.kt` |
| `webui/src/views/jmcomic/*`、`webui/src/views/bilibili-video/*` | 原生 UI（仅作交互参考） |

## 附录 B：配置项 → App 设置项

| 源配置 | 默认值 | 用途 | 放到哪 |
|---|---|---|---|
| `jm.password.zip` | `1234` | zip 密码 | JM 模式设置 |
| `jm.password.pdf` | `1234` | pdf 密码 | JM 模式设置 |
| `jm.download.threads` | `0`（=CPU 核数） | 单本下载并发 | JM 模式设置 |
| `jm.operation.parallel.enabled` | `false` | 不同 JM ID 是否并行 | JM 模式设置 |
| `jm.album.name_max_length` | `150` | 本子文件夹名上限（字节） | JM 模式设置 |
| `jm.api_domain` | `www.cdnbea.net` | JM API 域名 | JM 模式设置 |
| `jm.search.history.limit` | `20` | 在线搜索历史条数 | JM 模式设置 |
| `jm.request.album/search/chapter.retry` | `0` | 接口重试次数 | JM 模式设置 |
| `bilibili.cookies.sessdata` / `bili_jct` / `ticket` | 空 | B站鉴权 | **B站模式设置** |
| `bilibili.download_video.duration_limit` | `600` | 下载时长上限（秒） | **B站模式设置** |

> 没有 JM 账号相关配置项：本 App 不做 JM 登录（§5.1）。

## 附录 C：一句话验收标准（Definition of Done）

> 手机上：开屏选 JM → 搜到一本 → 入库 → **进漫画详情页（在线拉到章节列表，已下载的章节有标记）** → 下载（可整本可单章；图片解扰正确、进度精确到章节与图片、可取消、**中途杀进程重启能接着下**）→ **点章节进阅读器**（翻页/缩放/切章正常）→ 收藏 → 导出 zip 与 pdf（**弹密码框，默认填配置里的密码，也可选"不设密码"**；密码正确才能打开、错误密码打不开、书签正确）→ 切到 B站 → 在 B站设置里填好 Cookie → 粘贴链接入库 → 下载 → 播放 → 删除；全程**不需要任何服务器**，杀进程/断网/切后台都不丢数据。
