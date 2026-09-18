package com.haruhi.botServer.config;

import org.springframework.test.context.TestExecutionListeners;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 面向本机/受限执行环境的测试启动配置
 * <p>
 * 默认的 Spring Boot 测试监听器里包含 {@code ResetMocksTestExecutionListener}，它会在每个测试方法前后
 * 调用 Mockito 来重置 {@code @MockBean}。本项目这些测试根本不用 Mockito，而某些受限环境（如文件沙箱）
 * 下 Mockito 的 inline mock maker 无法自附加，会导致所有测试直接报错。
 * 这里显式只保留真正需要的监听器，从而完全不触发 Mockito 初始化。
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@TestExecutionListeners(
        listeners = {
                org.springframework.test.context.web.ServletTestExecutionListener.class,
                org.springframework.test.context.support.DirtiesContextBeforeModesTestExecutionListener.class,
                org.springframework.test.context.support.DependencyInjectionTestExecutionListener.class,
                org.springframework.test.context.support.DirtiesContextTestExecutionListener.class
        },
        mergeMode = TestExecutionListeners.MergeMode.REPLACE_DEFAULTS
)
public @interface NoMockitoSpringTest {
}
