# haruhibot-server

#### 介绍
1:java(springboot)基于gocqhttp,websocket反向实现的qq机器人(机器人:ws服务端,gocq:ws客户端);  
2:支持被多个gocqhttp连接;  
3:该机器人部署可与gocqhttp不在同一台服务器上,前提是gocq可以访问程序所在的主机(比如机器人部署在具有公网ip的服务器;或机器人部署在与gocq同一个局域网下的电脑上)  


#### 软件架构
SSM  
go-cqhttp  
websocket  
mybatis-plus  
dynamic-datasource  
maven  
mysql  


#### 安装/启动 教程

1.  安装:mysql5.7,java1.8(需要配置环境变量),maven3.8(需要配置环境变量)
2.  下载源码,双击`build.bat`(linux执行`build.sh`)
3.  解压 `target/haruhibotServer.zip`,双击`startup.bat`(linux执行`startup.sh`)
4.  gocqhttp配置好反向ws配置 `ws://ip:port/haruhi/ws`;配置`access-token`与机器人中的`access-token`一致(可不配置)
5.  只要配置对应ws地址和access-token的gocq都能连接,这样一个机器人后端可服务多个gocqhttp

#### 使用说明

1.  xxxx
2.  xxxx
3.  xxxx

#### 参与贡献



#### 特技


