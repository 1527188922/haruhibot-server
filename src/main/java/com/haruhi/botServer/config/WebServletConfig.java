package com.haruhi.botServer.config;

import com.haruhi.botServer.interceptors.ApiHeaderInterceptor;
import com.haruhi.botServer.interceptors.WebSocketHandshakeInterceptor;
import com.haruhi.botServer.utils.FileUtil;
import com.haruhi.botServer.ws.BotServer;
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

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(botServer, BotConfig.WEB_SOCKET_PATH)
                .addInterceptors(webSocketHandshakeInterceptor)
                .setAllowedOrigins("*");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(apiHeaderInterceptor)
                .addPathPatterns(BotConfig.CONTEXT_PATH+"/**")
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