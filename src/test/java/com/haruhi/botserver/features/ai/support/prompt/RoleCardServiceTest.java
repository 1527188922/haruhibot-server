package com.haruhi.botserver.features.ai.support.prompt;

import com.haruhi.botserver.configuration.service.Configs;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoleCardServiceTest {

    @TempDir
    Path dir;

    @BeforeEach
    void setUp() {
        Configs.useConfigDirForTest(dir.toString());
    }

    @AfterEach
    void tearDown() {
        Configs.resetConfigDirForTest();
    }

    @Test
    void loadsTriggerWordsAndFiltersModeSections() throws Exception {
        Path roles = dir.resolve("aichat/roles");
        Files.createDirectories(roles);
        Files.writeString(roles.resolve("小鲸鱼.md"), """
                # 角色卡：小鲸鱼

                触发词：小鲸鱼,鲸鱼,大肥鱼

                ## 一、你是谁
                你是小鲸鱼。

                ## 二、工具说明 〔有工具〕
                工具段

                ## 三、视觉说明 〔有视觉〕
                视觉段
                """);

        RoleCardService service = new RoleCardService();
        RoleCardService.RoleCard role = service.load("小鲸鱼");
        assertNotNull(role);
        assertTrue(role.triggerWords().contains("小鲸鱼"));
        assertFalse(role.content().contains("触发词"));
        assertTrue(role.content().contains("你是小鲸鱼"));
        assertTrue(role.content().contains("工具段"));
        assertFalse(role.content().contains("视觉段"));
        assertTrue(role.matches("小鲸鱼在吗"));
    }

    @Test
    void fallsBackToRoleNameWhenTriggerLineMissing() throws Exception {
        Path roles = dir.resolve("aichat/roles");
        Files.createDirectories(roles);
        Files.writeString(roles.resolve("春日酱.md"), """
                # 角色卡：春日酱

                ## 你是谁
                你是春日酱。
                """);

        RoleCardService.RoleCard role = new RoleCardService().load("春日酱");
        assertNotNull(role);
        assertTrue(role.triggerWords().contains("春日酱"));
    }

    @Test
    void rejectsPathTraversal() {
        RoleCardService service = new RoleCardService();
        assertNull(service.load("../secret"));
        assertNull(service.load("a/b"));
    }

    @Test
    void savesStructuredSectionsAndCopies() throws Exception {
        Path roles = dir.resolve("aichat/roles");
        Files.createDirectories(roles);
        Files.writeString(roles.resolve("小鲸鱼.md"), """
                # 角色卡：小鲸鱼

                触发词：小鲸鱼,鲸鱼

                ## 你是谁
                你是小鲸鱼。
                """);
        RoleCardService service = new RoleCardService();
        RoleCardService.RoleDefinition definition = service.definition("小鲸鱼");
        assertNotNull(definition);
        assertEquals(2, definition.triggers().size());
        assertEquals(1, definition.sections().size());

        RoleCardService.RoleSaveResult result = service.save(new RoleCardService.RoleSaveCommand(
                "小鲸鱼", "小鲸鱼", definition.triggers(),
                java.util.List.of(
                        new RoleCardService.RoleSection("你是谁", "base", java.util.List.of("你是小鲸鱼。")),
                        new RoleCardService.RoleSection("说话风格", "base", java.util.List.of("短句。"))),
                definition.mtime(), definition.hash(), false));
        assertTrue(result.saved());
        String saved = Files.readString(roles.resolve("小鲸鱼.md"));
        assertTrue(saved.contains("## 说话风格"));
        assertTrue(saved.contains("短句。"));

        RoleCardService.RoleDefinition copy = service.copy("小鲸鱼", "小鲸鱼-副本", false);
        assertNotNull(copy);
        assertEquals("小鲸鱼-副本", copy.name());
        assertEquals(Files.readString(roles.resolve("小鲸鱼.md")),
                Files.readString(roles.resolve("小鲸鱼-副本.md")));
        assertNotNull(service.suggestCopyName("小鲸鱼"));
    }
}
