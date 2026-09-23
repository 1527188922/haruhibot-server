package com.haruhi.botserver.bootstrap;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.MapPropertySource;
import org.springframework.mock.env.MockEnvironment;

import java.util.Map;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.io.TempDir;
import com.haruhi.botserver.configuration.metadata.ConfigKey;
import com.haruhi.botserver.configuration.service.Configs;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LegacyLoggingEnvironmentPostProcessorTest {
    @TempDir Path directory;
    private static final String OLD = "logging.level.com.haruhi.botServer";
    private static final String CURRENT = "logging.level.com.haruhi.botserver";

    @Test
    void configManagementReadsLegacyExternalSetting() throws Exception {
        Files.writeString(directory.resolve("application.yml"), "logging:\n  level:\n    com.haruhi.botServer: debug\n");
        try {
            Configs.useConfigDirForTest(directory.toString());
            assertEquals("debug", Configs.getStr(ConfigKey.LOGGING_LEVEL));
        } finally {
            Configs.resetConfigDirForTest();
        }
    }

    @Test
    void externalLegacySettingOverridesPackagedDefault() {
        MockEnvironment environment = new MockEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("external", Map.of(OLD, "debug")));
        environment.getPropertySources().addLast(new MapPropertySource("defaults", Map.of(CURRENT, "info")));
        new LegacyLoggingEnvironmentPostProcessor().postProcessEnvironment(environment, new SpringApplication());
        assertEquals("debug", environment.getProperty(CURRENT));
    }

    @Test
    void currentNameWinsWithinSamePropertySource() {
        MockEnvironment environment = new MockEnvironment().withProperty(OLD, "debug").withProperty(CURRENT, "warn");
        new LegacyLoggingEnvironmentPostProcessor().postProcessEnvironment(environment, new SpringApplication());
        assertEquals("warn", environment.getProperty(CURRENT));
    }

    @Test
    void higherPriorityCurrentNameWins() {
        MockEnvironment environment = new MockEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("commandLine", Map.of(CURRENT, "error")));
        environment.getPropertySources().addLast(new MapPropertySource("external", Map.of(OLD, "debug")));
        new LegacyLoggingEnvironmentPostProcessor().postProcessEnvironment(environment, new SpringApplication());
        assertEquals("error", environment.getProperty(CURRENT));
    }
}
