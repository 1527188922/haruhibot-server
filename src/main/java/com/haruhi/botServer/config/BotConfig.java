package com.haruhi.botServer.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class BotConfig {

    public static String DEFAULT_NAME = "春日酱";
    public static String INTERNET_HOST;
    public static int PORT;
    public final static String CONTEXT_PATH = "/api";
    public final static String WEB_SOCKET_PATH = CONTEXT_PATH + "/ws";
    public static boolean SAME_MACHINE_QQCLIENT;

    @Autowired
    public void setPort(@Value("${server.port}") int port) {
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

}

