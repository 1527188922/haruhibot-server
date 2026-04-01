package com.haruhi.botServer.config;

import cn.hutool.core.text.StrFormatter;
import com.alibaba.druid.support.jakarta.StatViewServlet;
import com.alibaba.druid.support.jakarta.WebStatFilter;
import com.alibaba.druid.support.spring.stat.DruidStatInterceptor;
import com.haruhi.botServer.condition.DruidEnabledCondition;
import com.haruhi.botServer.condition.DruidMonitorSpringCondition;
import com.haruhi.botServer.condition.DruidMonitorUrlCondition;
import com.haruhi.botServer.utils.FileUtil;
import com.haruhi.botServer.utils.PropertiesUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.aop.Advisor;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.JdkRegexpMethodPointcut;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;


@Configuration
@Conditional(DruidEnabledCondition.class)
public class DruidConfig {

    @Bean
    public ServletRegistrationBean<StatViewServlet> druidServlet() {
        ServletRegistrationBean<StatViewServlet> bean = new ServletRegistrationBean<>(new StatViewServlet(), StrFormatter.format("{}/*",BotConfig.DRUID_PATH));

        String username = PropertiesUtil.getProperty(FileUtil.FILE_NAME_WEBUI_CONFIG, PropertiesUtil.PROP_KEY_WEBUI_LOGIN_USERNAME);
        String password = PropertiesUtil.getProperty(FileUtil.FILE_NAME_WEBUI_CONFIG, PropertiesUtil.PROP_KEY_WEBUI_LOGIN_PASSWORD);
        // 登录验证
        if (StringUtils.isNotBlank(username) && StringUtils.isNotBlank(password)) {
            bean.addInitParameter("loginUsername", username);
            bean.addInitParameter("loginPassword", password);
        }

        // IP白名单
//        bean.addInitParameter("allow", "127.0.0.1");

        // IP黑名单（存在共同时，deny优先于allow）
        // bean.addInitParameter("deny", "192.168.1.100");
        return bean;
    }

    // ===================== 开启 Web监控(URL) + Session监控 =====================
    @Bean
    @Conditional(DruidMonitorUrlCondition.class)
    public FilterRegistrationBean<WebStatFilter> webStatFilter() {
        FilterRegistrationBean<WebStatFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(new WebStatFilter());
        bean.setName("druidWebStatFilter");
        bean.addUrlPatterns("/*");

        bean.addInitParameter("exclusions", "*.js,*.css,*.jpg,*.png,/druid/*,*.ico,/index.html");
        bean.addInitParameter("sessionStatEnable", PropertiesUtil.getProperty(FileUtil.FILE_NAME_WEBUI_CONFIG, PropertiesUtil.PROP_KEY_WEBUI_DRUID_MONITOR_URL_SESSION_ENABLED));
        bean.addInitParameter("sessionStatMaxCount", "1000");

        bean.setAsyncSupported(true);
        bean.setEnabled(true);
        return bean;
    }

    // ===================== 开启 Spring 监控 =====================
    @Bean
    @Conditional(DruidMonitorSpringCondition.class)
    public DruidStatInterceptor druidStatInterceptor() {
        return new DruidStatInterceptor();
    }

    @Bean
    @Scope("prototype")
    @Conditional(DruidMonitorSpringCondition.class)
    public JdkRegexpMethodPointcut druidStatPointcut() {
        JdkRegexpMethodPointcut pointcut = new JdkRegexpMethodPointcut();
        pointcut.setPatterns("com.haruhi.botServer.controller..*",
                "com.haruhi.botServer.mapper..*",
                "com.haruhi.botServer.service..*",
                "com.haruhi.botServer.handlers..*",
                "com.haruhi.botServer.ws..*");
        return pointcut;
    }

    @Bean
    @Conditional(DruidMonitorSpringCondition.class)
    public Advisor druidStatAdvisor() {
        return new DefaultPointcutAdvisor(druidStatPointcut(), druidStatInterceptor());
    }
}