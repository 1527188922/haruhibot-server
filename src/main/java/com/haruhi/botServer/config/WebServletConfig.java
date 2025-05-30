package com.haruhi.botServer.config;

import com.haruhi.botServer.interceptors.HeaderInterceptor;
import com.haruhi.botServer.interceptors.WebSocketHandshakeInterceptor;
import com.haruhi.botServer.utils.FileUtil;
import com.haruhi.botServer.ws.Server;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@EnableWebSocket
@Configuration
@Slf4j
public class WebServletConfig implements WebSocketConfigurer, WebMvcConfigurer {
    
    @Autowired
    private HeaderInterceptor headerInterceptors;
    @Autowired
    private Server server;
    @Autowired
    private WebSocketHandshakeInterceptor webSocketHandshakeInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(server, BotConfig.CONTEXT_PATH+"/ws")
                .addInterceptors(webSocketHandshakeInterceptor)
                .setAllowedOrigins("*");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(headerInterceptors)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/error","/css/**", "/js/**", "/index.html", "/img/**", "/fonts/**", "/favicon.ico","/svg/**");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("file:"+ FileUtil.getAppDir() + "/")
                .addResourceLocations("classpath:/webui/")
                .setCachePeriod(0);
        log.info("映射本地路径：{}",FileUtil.getAppDir());
    }
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // 对于所有非API请求，返回index.html
        registry.addViewController("/").setViewName("forward:/index.html");
        registry.addViewController("/{path:[^\\.]*}").setViewName("forward:/index.html");
        registry.setOrder(Ordered.HIGHEST_PRECEDENCE);
    }
}