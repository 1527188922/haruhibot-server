package com.haruhi.botServer.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:config/webuiConfig.properties")
@Data
public class WebuiConfig {

    @Value("${login.username}")
    private String loginUserName;
    @Value("${login.password}")
    private String loginPassword;
    // 单位分钟
    @Value("${login.expire:30}")
    private Long loginExpire;


    @Value("${druid.enabled}")
    private String druidEnabled;
    @Value("${druid.loginUsername}")
    private String druidLoginUsername;
    @Value("${druid.loginPassword}")
    private String druidLoginPassword;

}
