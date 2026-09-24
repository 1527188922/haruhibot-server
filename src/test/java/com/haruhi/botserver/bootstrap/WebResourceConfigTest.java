package com.haruhi.botserver.bootstrap;

import com.haruhi.botserver.configuration.metadata.ConfigKey;
import com.haruhi.botserver.configuration.service.ConfigHub;
import com.haruhi.botserver.configuration.service.Configs;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class WebResourceConfigTest {
    @TempDir Path directory;
    private final AtomicReference<String> publicIp = new AtomicReference<>("203.0.113.10");
    private final AtomicInteger publicIpCalls = new AtomicInteger();
    private final AtomicInteger localIpCalls = new AtomicInteger();
    private GenericApplicationContext context;
    private ConfigHub hub;
    private WebResourceConfig resources;

    @BeforeEach
    void setUp() throws Exception {
        Files.writeString(directory.resolve("application.yml"), "server:\n  port: 8090\ninternet-host: first.example\n");
        Configs.useConfigDirForTest(directory.toString());
        resources = new WebResourceConfig(() -> {
            publicIpCalls.incrementAndGet();
            return publicIp.get();
        }, () -> {
            localIpCalls.incrementAndGet();
            return "192.0.2.20";
        });
        context = new GenericApplicationContext();
        context.getBeanFactory().registerSingleton("webResourceConfig", resources);
        context.refresh();
        hub = new ConfigHub();
        ReflectionTestUtils.setField(hub, "applicationContext", context);
    }

    @AfterEach
    void tearDown() {
        if (context != null) context.close();
        Configs.resetConfigDirForTest();
    }

    @Test
    void savingHostUpdatesBaseAndDerivedAddresses() {
        assertEquals("http://first.example:8090", resources.webHomePath());
        assertTrue(hub.save(ConfigKey.INTERNET_HOST, "second.example"));
        assertEquals("http://second.example:8090", resources.webHomePath());
        assertTrue(resources.webResourcesImagePath().startsWith("http://second.example:8090/"));
        assertTrue(resources.webResourcesAudioPath().startsWith("http://second.example:8090/"));
        assertEquals(0, publicIpCalls.get());
    }

    @Test
    void clearingHostRunsDefaultLookupAgainEachTime() {
        hub.save(ConfigKey.INTERNET_HOST, "");
        assertEquals("http://203.0.113.10:8090", resources.webHomePath());
        assertEquals(0, localIpCalls.get());
        hub.save(ConfigKey.INTERNET_HOST, "custom.example");
        publicIp.set("203.0.113.11");
        hub.save(ConfigKey.INTERNET_HOST, "");
        assertEquals("http://203.0.113.11:8090", resources.webHomePath());
        assertEquals(2, publicIpCalls.get());
    }

    @Test
    void blankHostAndMissingPublicIpFallBackToLocalIp() {
        publicIp.set("");
        hub.save(ConfigKey.INTERNET_HOST, "   ");
        assertEquals("http://192.0.2.20:8090", resources.webHomePath());
        assertEquals(1, publicIpCalls.get());
        assertEquals(1, localIpCalls.get());
    }

    @Test
    void unchangedHostDoesNotRepeatIpLookup() {
        hub.save(ConfigKey.INTERNET_HOST, "");
        hub.save(ConfigKey.INTERNET_HOST, "");
        assertEquals(1, publicIpCalls.get());
    }

    @Test
    void changingHostDoesNotApplyRestartOnlyPort() {
        hub.save(ConfigKey.SERVER_PORT, "8099");
        hub.save(ConfigKey.INTERNET_HOST, "next.example");
        assertEquals("http://next.example:8090", resources.webHomePath());
    }
}
