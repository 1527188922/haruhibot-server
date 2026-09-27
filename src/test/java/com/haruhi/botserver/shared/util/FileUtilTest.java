package com.haruhi.botserver.shared.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 文件名跨平台兼容处理测试
 */
class FileUtilTest {

    /**
     * 日志中真实出现的本子名，含 '*'（windows非法）和 '〜'（合法但多字节）
     */
    private static final String REAL_ALBUM_NAME =
            "[白杨汉化组] (C102) [むらさきいろのよる (むらさき*)] 先生、これは2人だけのヒミツですよ〜セーラー服の秘密〜[中国翻译] [DL版]";

    @Test
    void replacesWindowsInvalidCharsWithDash() {
        assertEquals("a-b", FileUtil.sanitizeFileName("a*b", "fallback", 0));
        assertEquals("a-b", FileUtil.sanitizeFileName("a?b", "fallback", 0));
        assertEquals("a-b", FileUtil.sanitizeFileName("a\"b", "fallback", 0));
        assertEquals("a-b", FileUtil.sanitizeFileName("a<b", "fallback", 0));
        assertEquals("a-b", FileUtil.sanitizeFileName("a>b", "fallback", 0));
        assertEquals("a-b", FileUtil.sanitizeFileName("a|b", "fallback", 0));
        assertEquals("a-b", FileUtil.sanitizeFileName("a:b", "fallback", 0));
        // 目录分隔符也要替换，否则会被当成路径分隔符落到别的目录
        assertEquals("a-b", FileUtil.sanitizeFileName("a/b", "fallback", 0));
        assertEquals("a-b", FileUtil.sanitizeFileName("a\\b", "fallback", 0));
    }

    @Test
    void removesControlCharsAndTrimsBlankEnds() {
        assertEquals("ab", FileUtil.sanitizeFileName("a\u0000\u0001\u001Fb", "fallback", 0));
        assertEquals("ab", FileUtil.sanitizeFileName("  ab  ", "fallback", 0));
        // windows下以点结尾无法创建
        assertEquals("ab", FileUtil.sanitizeFileName("ab...", "fallback", 0));
        assertEquals("ab", FileUtil.sanitizeFileName("ab. . ", "fallback", 0));
    }

    @Test
    void keepsMultiByteCharsIntact() {
        assertEquals(REAL_ALBUM_NAME.replace("*", "-"),
                FileUtil.sanitizeFileName(REAL_ALBUM_NAME, "fallback", 0));
    }

    @Test
    void usesFallbackWhenNothingLeft() {
        assertEquals("480854", FileUtil.sanitizeFileName("*", "480854", 0));
        assertEquals("480854", FileUtil.sanitizeFileName("***", "480854", 0));
        assertEquals("480854", FileUtil.sanitizeFileName(null, "480854", 0));
        assertEquals("480854", FileUtil.sanitizeFileName("   ", "480854", 0));
        // 兜底名字也清洗
        assertEquals("unnamed", FileUtil.sanitizeFileName(null, null, 0));
    }

    @Test
    void suffixWindowsReservedDeviceName() {
        assertEquals("CON_", FileUtil.sanitizeFileName("CON", "fallback", 0));
        assertEquals("nul_", FileUtil.sanitizeFileName("nul", "fallback", 0));
        // 带后缀的不算保留名
        assertEquals("CON_JM1", FileUtil.sanitizeFileName("CON_JM1", "fallback", 0));
    }

    @Test
    void truncatesOnCharBoundaryAndRespectsByteLimit() {
        String truncated = FileUtil.sanitizeFileName(REAL_ALBUM_NAME, "fallback", 30);
        assertTrue(truncated.getBytes(StandardCharsets.UTF_8).length <= 30,
                "截断后不应超过字节上限，实际：" + truncated.getBytes(StandardCharsets.UTF_8).length);
        assertTrue(REAL_ALBUM_NAME.startsWith(truncated), "截断应保留原名前缀：" + truncated);

        // 3字节的日文不能只截一半：上限10字节最多放3个字符
        String multibyte = FileUtil.sanitizeFileName("あああああ", "fallback", 10);
        assertEquals("あああ", multibyte);
        assertEquals(9, multibyte.getBytes(StandardCharsets.UTF_8).length);
    }

    /**
     * 清洗后的名字必须真的能落盘，这是本次问题的根本诉求
     */
    @Test
    void sanitizedNamesAreCreatable(@TempDir Path tempDir) throws IOException {
        String[] rawNames = {
                REAL_ALBUM_NAME,
                "标题：副标题*第1话?",
                "a|b<c>d\"e",
                "CON",
                "trailing. ",
                "第1话\u0001"
        };
        for (String raw : rawNames) {
            String name = FileUtil.sanitizeFileName(raw, "fallback", 150);
            File dir = tempDir.resolve(name).toFile();
            assertTrue(dir.mkdirs(), "创建文件夹失败：" + name);
            // 模拟子目录+图片文件，与 JmcomicService 的落盘路径一致
            File image = new File(new File(dir, "第1话"), "00001.webp.tmp");
            assertTrue(image.getParentFile().mkdirs(), "创建章节文件夹失败：" + name);
            assertTrue(image.createNewFile(), "创建文件失败：" + image.getAbsolutePath());
            assertFalse(name.contains("*") || name.contains(":") || name.contains("?"));
        }
    }

    /**
     * windows下 getCanonicalPath 会校验名字合法性，非法字符直接抛 IOException("Bad pathname")
     * 这正是线上报错 "文件名、目录名或卷标语法不正确" 的来源
     */
    @Test
    @EnabledOnOs(OS.WINDOWS)
    void sanitizedNamesPassWindowsCanonicalize(@TempDir Path tempDir) throws IOException {
        String name = FileUtil.sanitizeFileName(REAL_ALBUM_NAME, "480854", 150) + "_JM480854";
        String path = tempDir.resolve(name).resolve("第1话").resolve("00001.webp.tmp").toString();
        // 不抛异常即为通过
        assertTrue(new File(path).getCanonicalPath().contains("第1话"));
    }
}
