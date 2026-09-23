//package com.haruhi.botserver.configuration.service;
//
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.context.ApplicationContextInitializer;
//import org.springframework.context.ConfigurableApplicationContext;
//import org.springframework.core.env.MapPropertySource;
//
///**
// * 把 {@code ./config/} 下的配置（properties + yml）注入 Spring Environment
// * <p>
// * 这样 {@code @ConditionalOnProperty}、{@code @Value}、第三方 starter 的属性绑定与
// * {@link Configs} 读到的是同一份配置，且不依赖 {@code spring.config.import} 的工作目录解析。
// * <p>
// * 注册方式见 {@code src/main/resources/META-INF/spring.factories}。
// */
//@Slf4j
//public class ConfigsEnvironmentInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
//
//    /** 属性源名称，放在最前面以保证优先级最高 */
//    public static final String PROPERTY_SOURCE_NAME = "haruhibotConfigFiles";
//
//    @Override
//    public void initialize(ConfigurableApplicationContext applicationContext) {
//        try {
//            MapPropertySource propertySource = new MapPropertySource(PROPERTY_SOURCE_NAME, Configs.loadIntoEnvironment());
//            applicationContext.getEnvironment().getPropertySources().addFirst(propertySource);
//        } catch (Exception e) {
//            log.error("注入配置文件到Spring Environment失败，将只依赖 Configs 快照", e);
//        }
//    }
//}
