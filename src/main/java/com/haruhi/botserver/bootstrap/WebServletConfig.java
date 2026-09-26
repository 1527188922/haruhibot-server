package com.haruhi.botserver.bootstrap;

import com.haruhi.botserver.infrastructure.web.interceptor.ApiHeaderInterceptor;
import com.haruhi.botserver.infrastructure.web.interceptor.WebSocketHandshakeInterceptor;
import com.haruhi.botserver.infrastructure.web.websocket.WebuiWsHandler;
import com.haruhi.botserver.infrastructure.web.websocket.WebuiWsHandshakeInterceptor;
import com.haruhi.botserver.shared.util.FileUtil;
import com.haruhi.botserver.integration.onebot.websocket.BotServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@EnableWebSocket
@Configuration
@Slf4j
public class WebServletConfig implements WebSocketConfigurer, WebMvcConfigurer {
    
    @Autowired
    private ApiHeaderInterceptor apiHeaderInterceptor;
    @Autowired
    private BotServer botServer;
    @Autowired
    private WebSocketHandshakeInterceptor webSocketHandshakeInterceptor;
    @Autowired
    private WebuiWsHandler webuiWsHandler;
    @Autowired
    private WebuiWsHandshakeInterceptor webuiWsHandshakeInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 机器人反向ws(gocq)
        registry.addHandler(botServer, SysConstants.WEB_SOCKET_PATH)
                .addInterceptors(webSocketHandshakeInterceptor)
                .setAllowedOrigins("*");
        // webui 全局实时消息总线，鉴权用登录token(?token=)
        registry.addHandler(webuiWsHandler, SysConstants.WEBUI_WEB_SOCKET_PATH)
                .addInterceptors(webuiWsHandshakeInterceptor)
                .setAllowedOrigins("*");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(apiHeaderInterceptor)
                .addPathPatterns(SysConstants.CONTEXT_PATH+"/**")
        ;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("file:"+ FileUtil.getAppDir() + "/")
                .addResourceLocations("classpath:/webui/")
                .setCachePeriod(0);
        log.info("映射本地路径：{}",FileUtil.getAppDir());
    }
}