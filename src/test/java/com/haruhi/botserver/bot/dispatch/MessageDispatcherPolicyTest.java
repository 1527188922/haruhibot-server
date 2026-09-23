package com.haruhi.botserver.bot.dispatch;

import com.alibaba.fastjson.JSON;
import com.haruhi.botserver.bot.handler.IMessageHandler;
import com.haruhi.botserver.configuration.service.Configs;
import com.haruhi.botserver.features.chatrecord.handler.ChatRecordHandler;
import com.haruhi.botserver.integration.onebot.model.Message;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MessageDispatcherPolicyTest {
    @TempDir Path directory;

    @AfterEach void reset() { Configs.resetConfigDirForTest(); }

    @Test
    void dispatcherRegistriesAreIsolatedBetweenContexts() {
        IMessageHandler handler = new IMessageHandler() {
            public int weight() { return 1; }
            public String funName() { return "example"; }
        };
        MessageDispatcher first = new MessageDispatcher(Map.of("example", handler));
        MessageDispatcher second = new MessageDispatcher(Map.of("example", handler));
        assertEquals(1, first.getContainer().size());
        assertEquals(1, second.getContainer().size());
        assertNotSame(first.getContainer(), second.getContainer());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void declaredBackgroundHandlerBypassesGroupRestrictions(boolean disabled) throws Exception {
        Files.createDirectories(directory.resolve("config"));
        Files.writeString(directory.resolve("config/bot.properties"), "bot.switch.disable_group=" + disabled + "\nbot.access_groups=999\n");
        Configs.useConfigDirForTest(directory.toString());
        Message message = JSON.parseObject("{\"message_type\":\"group\",\"group_id\":123,\"user_id\":1,\"self_id\":2}", Message.class);
        MessageDispatcher dispatcher = new MessageDispatcher(Map.of());
        IMessageHandler ordinary = new IMessageHandler() {
            public int weight() { return 1; }
            public String funName() { return "ordinary"; }
        };
        IMessageHandler background = new IMessageHandler() {
            public int weight() { return 2; }
            public String funName() { return "background"; }
            public boolean bypassGroupRestrictions() { return true; }
        };
        assertEquals(true, ReflectionTestUtils.invokeMethod(dispatcher, "toContinue", ordinary, message));
        assertEquals(false, ReflectionTestUtils.invokeMethod(dispatcher, "toContinue", background, message));
        assertEquals(false, ReflectionTestUtils.invokeMethod(dispatcher, "toContinue", new ChatRecordHandler(), message));
    }
}
