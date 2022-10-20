package com.haruhi.server.config;

import com.haruhi.server.interceptors.WebSocketServerInterceptor;
import com.haruhi.server.ws.ServerHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@EnableWebSocket
@Configuration
public class WebSocketConfig implements WebSocketConfigurer {

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new ServerHandler(),"/ws")
                .addInterceptors(new WebSocketServerInterceptor())
                .setAllowedOrigins("*");
    }
}