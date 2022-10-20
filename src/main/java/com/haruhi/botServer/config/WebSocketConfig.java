package com.haruhi.botServer.config;

import com.haruhi.botServer.interceptors.WebSocketServerInterceptor;
import com.haruhi.botServer.ws.ServerEndpoint;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@EnableWebSocket
@Configuration
public class WebSocketConfig implements WebSocketConfigurer {

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new ServerEndpoint(),"/ws")
                .addInterceptors(new WebSocketServerInterceptor())
                .setAllowedOrigins("*");
    }
}