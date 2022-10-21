package com.haruhi.botServer.config;

import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DataBaseConfig {

    // bot数据源名称
    public final static String DATA_SOURCE_MASTER = "master";
    // 系统数据源名称
    public final static String DATA_SOURCE_SYSTEM = "system";
    // bot数据库名称
    public static String DATA_BASE_BOT = "";
    // 数据库用户名
    public static String DATA_BASE_BOT_USERNAME = "";
    // 数据库密码
    public static String DATA_BASE_BOT_PASSWORD = "";
    //  数据库 host
    public static String DATA_BASE_BOT_HOST = "";
    // 数据库 port
    public static String DATA_BASE_BOT_PORT = "";


    // 群聊天历史
    public final static String T_GROUP_CHAT_HISTORY = "t_group_chat_history";

    @Autowired
    public void setBotDbname(@Value("${mysql.dbName}") String dbName) {
        DATA_BASE_BOT = dbName;
        if (Strings.isBlank(DATA_BASE_BOT)) {
            throw new IllegalArgumentException("未配置数据库名称！");
        }
    }

    @Autowired
    public void setUsername(@Value("${mysql.username}") String username) {
        DATA_BASE_BOT_USERNAME = username;
        if (Strings.isBlank(DATA_BASE_BOT_USERNAME)) {
            throw new IllegalArgumentException("未配置数据库用户名！");
        }
    }
    @Autowired
    public void setPassword(@Value("${mysql.password}") String password) {
        DATA_BASE_BOT_PASSWORD = password;
        if (Strings.isBlank(DATA_BASE_BOT_PASSWORD)) {
            throw new IllegalArgumentException("未配置数据库密码！");
        }
    }
    @Autowired
    public void setHost(@Value("${mysql.host}") String host) {
        DATA_BASE_BOT_HOST = host;
        if (Strings.isBlank(DATA_BASE_BOT_HOST)) {
            throw new IllegalArgumentException("未配置数据库主机地址！");
        }
    }
    @Autowired
    public void setPort(@Value("${mysql.port}") String port) {
        DATA_BASE_BOT_PORT = port;
        if (Strings.isBlank(DATA_BASE_BOT_PORT)) {
            throw new IllegalArgumentException("未配置数据库端口！");
        }
    }

}
