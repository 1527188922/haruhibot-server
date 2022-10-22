package com.haruhi.botServer.config;

import com.haruhi.botServer.config.path.AbstractPathConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Slf4j
@Configuration
public class WebResourceConfig implements WebMvcConfigurer {

    @Autowired
    private AbstractPathConfig abstractPathConfig;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String s = abstractPathConfig.resourceHomePath();
        registry.addResourceHandler("/**").addResourceLocations("file:"+ s + "/");
        log.info("映射本地路径：{}",s);
    }
}
