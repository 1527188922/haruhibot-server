# haruhibot-server

#### 介绍
1：一个java开发的QQ Bot  
2：使用[NapCat](https://napneko.github.io)于QQ交互


#### 软件架构
Spring boot  
WebSocket  
Mybatis-Plus  
Dynamic-datasource  
Maven  
Sqlite  


#### 安装/启动 教程

* Bot安装/启动  
  1. 安装:`java1.8`、`maven3.8`
  2. 下载bot源码后,双击打包脚本`build.bat`(linux执行`build.sh`)  
  3. 执行打包脚本完后会出现文件：`target/haruhibotServer.zip`，解压后双击启动脚本：`startup.bat`(linux执行`startup.sh`)
* Bot Webui安装/启动（可选步骤）
  1. 安装:`nodejs`
  2. 进入目录：`webui`，执行安装依赖命令：`npm install`，再执行打包命令：`npm run build`
  3. 打包后会出现目录：`./webui/dist`
  4. 将`dist`中的文件全选，复制进Bot压缩包解压后的目录：`./haruhibotServer/webui`中去
  5. 访问webui地址：`http://{ip}:{port}`，webui账户密码配置文件：`./haruhibotServer/config/webuiConfig.properties`
* NapCat启动
  1. 看NapCat官方教程进行安装：https://napneko.github.io
  2. 配置反向WebSocket地址：`http://{ip}:{port}/api/ws`（反向：Bot作为服务端，NapCat作为客户端）


#### 使用说明

1.  xxxx
2.  xxxx
3.  xxxx

#### 参与贡献



#### 特技


