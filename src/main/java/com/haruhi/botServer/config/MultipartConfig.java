package com.haruhi.botServer.config;

import com.haruhi.botServer.utils.FileUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.servlet.MultipartConfigElement;
import java.io.File;

@Slf4j
@Configuration
public class MultipartConfig {

    @Bean
    public MultipartConfigElement multipartConfigElement() {
        MultipartConfigFactory factory = new MultipartConfigFactory();
        File tempFile = FileUtil.mkdirs(FileUtil.getAppTempDir());
        factory.setLocation(tempFile.getPath());
        log.info("自定义临时目录：{}",tempFile.getPath());
        return factory.createMultipartConfig();
    }
}
