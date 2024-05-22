package com.haruhi.botServer.config;

import com.haruhi.botServer.interceptors.HeaderInterceptor;
import com.haruhi.botServer.interceptors.WebSocketHandshakeInterceptor;
import com.haruhi.botServer.ws.Server;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@EnableWebSocket
@Configuration
public class WebSocketConfig implements WebSocketConfigurer, WebMvcConfigurer {
    
    @Value("${server.servlet.ws-path}")
    private String wsPath;
    @Autowired
    private HeaderInterceptor headerInterceptors;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new Server(), Strings.isBlank(wsPath) ? "/ws" : wsPath)
                .addInterceptors(new WebSocketHandshakeInterceptor())
                .setAllowedOrigins("*");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(headerInterceptors)
                .addPathPatterns("/**");
    }
}