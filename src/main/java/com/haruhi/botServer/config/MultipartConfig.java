package com.haruhi.botServer.config;

import com.haruhi.botServer.config.path.AbstractPathConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.servlet.MultipartConfigElement;
import java.io.File;

@Slf4j
@Configuration
public class MultipartConfig {

    @Autowired
    private AbstractPathConfig abstractPathConfig;

    @Bean
    public MultipartConfigElement multipartConfigElement() {
        MultipartConfigFactory factory = new MultipartConfigFactory();
        File file = abstractPathConfig.tempPath();
        if (!file.exists()) {
            file.mkdirs();
        }
        factory.setLocation(file.getPath());
        log.info("自定义临时目录：{}",file.getPath());
        return factory.createMultipartConfig();
    }
}
