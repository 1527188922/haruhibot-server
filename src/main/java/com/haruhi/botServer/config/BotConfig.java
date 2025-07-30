package com.haruhi.botServer.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class BotConfig {

    public static String NAME = "";
    public static Long DEFAULT_USER = null;
    public static String INTERNET_HOST;
    public static int PORT;
    public final static String CONTEXT_PATH = "/api";
    public final static String WEB_SOCKET_PATH = CONTEXT_PATH + "/ws";
    public static boolean SAME_MACHINE_QQCLIENT;
    // 是否启用公网ip 0否 1是 若程序和gocq都在同一台主机上 可以不启用
    public static String ENABLE_INTERNET_HOST;

    @Autowired
    public void setName(@Value("${bot.name}") String name) {
        NAME = Strings.isBlank(name) ? "春日酱" : name;
    }

    @Autowired
    public void setDefaultUser(@Value("${bot.default-user}") Long defaultUser) {
        DEFAULT_USER = defaultUser;
        if (DEFAULT_USER == null || 0L == DEFAULT_USER) {
            DEFAULT_USER = 1527188922L;
        }
    }

    @Autowired
    public void setPort(@Value("${bot.port}") int port) {
        PORT = port;
        if (PORT == 0) {
            throw new IllegalArgumentException();
        }
    }

    @Autowired
    public void setInternetHost(@Value("${bot.internet-host}") String internetHost) {
        INTERNET_HOST = internetHost;
    }

//    @Autowired
//    public void setContextPath(@Value("${server.servlet.context-path}") String contextPath){
//        CONTEXT_PATH = contextPath;
//    }

    @Autowired
    public void setSameMachineQqClient(@Value("${bot.same-machine-qqclient}") boolean sameMachineQqClient){
        SAME_MACHINE_QQCLIENT = sameMachineQqClient;
    }

    @Autowired
    public void setEnableInternetHost(@Value("${bot.enable-internet-host}") String enableInternetHost){
        ENABLE_INTERNET_HOST = enableInternetHost;
    }
}

