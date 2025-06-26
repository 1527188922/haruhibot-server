package com.haruhi.botServer.config;

import com.haruhi.botServer.interceptors.ApiHeaderInterceptor;
import com.haruhi.botServer.interceptors.WebSocketHandshakeInterceptor;
import com.haruhi.botServer.service.LoginService;
import com.haruhi.botServer.utils.FileUtil;
import com.haruhi.botServer.ws.BotServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
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
                .addPathPatterns("/api/**")
//                .excludePathPatterns("/error","/css/**", "/js/**", "/index.html", "/img/**", "/fonts/**", "/favicon.ico","/svg/**")
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
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // 对于所有非API请求，返回index.html
        registry.addViewController("/").setViewName("forward:/index.html");
        registry.addViewController("/{path:[^\\.]*}").setViewName("forward:/index.html");
        registry.setOrder(Ordered.HIGHEST_PRECEDENCE);
    }


//    @Bean
    public CorsFilter corsFilter(){
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOrigin("*");
        config.setAllowCredentials(true);
        config.addAllowedMethod("*");
        config.addAllowedHeader("*");

        config.addExposedHeader("Content-Type");
        config.addExposedHeader( "X-Requested-With");
        config.addExposedHeader("accept");
        config.addExposedHeader("Origin");
        config.addExposedHeader(LoginService.HEADER_KEY_AUTHORIZATION);
        config.addExposedHeader(LoginService.HEADER_KEY_USER_NAME);
        config.addExposedHeader( "Access-Control-Request-Method");
        config.addExposedHeader("Access-Control-Request-Headers");

        UrlBasedCorsConfigurationSource configSource = new UrlBasedCorsConfigurationSource();
        configSource.registerCorsConfiguration("/**", config);

        return new CorsFilter(configSource);
    }
}