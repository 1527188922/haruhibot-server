package com.haruhi.botserver.bootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;

import java.util.Map;

/** Keeps existing external logging settings effective after the package rename. */
public class LegacyLoggingEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {
    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String current = "logging.level.com.haruhi.botserver";
        String legacy = "logging.level.com.haruhi.botServer";
        for (PropertySource<?> source : environment.getPropertySources()) {
            // This synthetic source aggregates all others and hides their precedence.
            if (source.getName().equals("configurationProperties")) continue;
            if (source.getProperty(current) != null) return;
            Object value = source.getProperty(legacy);
            if (value != null) {
                environment.getPropertySources().addFirst(new MapPropertySource(
                        "legacyBotLogging", Map.of(current, value)));
                return;
            }
        }
    }

    @Override
    public int getOrder() { return ConfigDataEnvironmentPostProcessor.ORDER + 1; }
}
