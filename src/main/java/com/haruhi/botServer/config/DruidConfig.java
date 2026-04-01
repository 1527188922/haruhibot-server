package com.haruhi.botServer.config;

import cn.hutool.core.text.StrFormatter;
import com.alibaba.druid.support.jakarta.StatViewServlet;
import com.alibaba.druid.support.jakarta.WebStatFilter;
import com.alibaba.druid.support.spring.stat.DruidStatInterceptor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.aop.Advisor;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.JdkRegexpMethodPointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;


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

    // ===================== 开启 Web监控(URL) + Session监控 =====================
    @Bean
    public FilterRegistrationBean<WebStatFilter> webStatFilter() {
        FilterRegistrationBean<WebStatFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(new WebStatFilter());
        bean.setName("druidWebStatFilter");
        bean.addUrlPatterns("/*");

        bean.addInitParameter("exclusions", "*.js,*.css,*.jpg,*.png,/druid/*,*.ico,/index.html");
        bean.addInitParameter("sessionStatEnable", "true");
        bean.addInitParameter("sessionStatMaxCount", "1000");

        bean.setAsyncSupported(true);
        bean.setEnabled(true);
        return bean;
    }

    // ===================== 开启 Spring 监控 =====================
    @Bean
    public DruidStatInterceptor druidStatInterceptor() {
        return new DruidStatInterceptor();
    }

    @Bean
    @Scope("prototype")
    public JdkRegexpMethodPointcut druidStatPointcut() {
        JdkRegexpMethodPointcut pointcut = new JdkRegexpMethodPointcut();
        pointcut.setPatterns("com.haruhi.botServer.controller..*",
                "com.haruhi.botServer.handlers..*",
                "com.haruhi.botServer.ws..*");
        return pointcut;
    }

    @Bean
    public Advisor druidStatAdvisor() {
        return new DefaultPointcutAdvisor(druidStatPointcut(), druidStatInterceptor());
    }
}