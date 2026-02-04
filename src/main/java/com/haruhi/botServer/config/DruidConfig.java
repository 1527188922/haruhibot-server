package com.haruhi.botServer.config;

import cn.hutool.core.text.StrFormatter;
import com.alibaba.druid.support.jakarta.StatViewServlet;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DruidConfig {
    @Autowired
    private WebuiConfig webuiConfig;

    @Bean
    public ServletRegistrationBean<StatViewServlet> druidServlet() {
        ServletRegistrationBean<StatViewServlet> bean = new ServletRegistrationBean<>(new StatViewServlet(), StrFormatter.format("{}/*",BotConfig.DRUID_PATH));

        // 登录验证
        if (StringUtils.isNotBlank(webuiConfig.getDruidLoginUsername()) && StringUtils.isNotBlank(webuiConfig.getDruidLoginPassword())) {
            bean.addInitParameter("loginUsername", webuiConfig.getDruidLoginUsername());
            bean.addInitParameter("loginPassword", webuiConfig.getDruidLoginPassword());
        }

        // IP白名单
//        bean.addInitParameter("allow", "127.0.0.1");

        // IP黑名单（存在共同时，deny优先于allow）
        // bean.addInitParameter("deny", "192.168.1.100");
        return bean;
    }
}