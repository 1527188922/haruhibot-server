package com.haruhi.botserver.features.jmcomic.service;

import com.haruhi.botserver.configuration.metadata.ConfigKey;
import com.haruhi.botserver.configuration.service.Configs;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 本子文件夹名称构建测试
 * <p>
 * 文件夹名会直接落盘，必须同时兼容windows和linux，见 {@link JmcomicService#buildAlbumFolderName}
 */
class JmcomicAlbumFolderNameTest {

    private static final String AID = "480854";

    /**
     * 线上报错的本子名，'*' 为windows非法字符，会导致 File.getCanonicalPath 抛 "Bad pathname"
     */
    private static final String REAL_ALBUM_NAME =
            "[白杨汉化组] (C102) [むらさきいろのよる (むらさき*)] 先生、これは2人だけのヒミツですよ〜セーラー服の秘密〜[中国翻译] [DL版]";

    private final TestJmcomicService service = new TestJmcomicService();

    @Test
    void folderNameContainsNoWindowsInvalidChar() {
        String folderName = service.buildAlbumFolderName(REAL_ALBUM_NAME, AID);

        assertFalse(folderName.contains("*"), "windows非法字符 '*' 必须被替换：" + folderName);
        assertTrue(folderName.endsWith("_JM" + AID), "文件夹名必须以jm号结尾：" + folderName);
        assertTrue(REAL_ALBUM_NAME.contains("セーラー服"), "内容应保留：" + folderName);
        // 多字节字符不能被截坏
        assertFalse(folderName.contains("\uFFFD"), "不应出现乱码替换字符：" + folderName);
    }

    @Test
    void folderNameFitsFileSystemNameLimit() {
        int maxBytes = Configs.getInt(ConfigKey.JM_ALBUM_NAME_MAX_LENGTH, 150);
        StringBuilder longName = new StringBuilder();
        for (int i = 0; i < 200; i++) {
            longName.append("あ");
        }

        String folderName = service.buildAlbumFolderName(longName.toString(), AID);

        int actualBytes = folderName.getBytes(StandardCharsets.UTF_8).length;
        assertTrue(actualBytes <= maxBytes,
                "文件夹名字节数应不超过配置上限" + maxBytes + "，实际：" + actualBytes);
        // windows/linux 单个名字上限均为255字节
        assertTrue(actualBytes < 255, "文件夹名必须小于255字节，实际：" + actualBytes);
        assertTrue(folderName.endsWith("_JM" + AID), "后缀不参与截断：" + folderName);
    }

    @Test
    void folderNameFallsBackToAidWhenNameBlank() {
        assertEquals(AID + "_JM" + AID, service.buildAlbumFolderName(null, AID));
        assertEquals(AID + "_JM" + AID, service.buildAlbumFolderName("", AID));
        // 全是非法字符时同样兜底为jm号
        assertEquals(AID + "_JM" + AID, service.buildAlbumFolderName("***", AID));
    }

    @Test
    void folderNameReplacesPathSeparator() {
        String folderName = service.buildAlbumFolderName("a/b\\c", AID);
        assertFalse(folderName.contains("/"));
        assertFalse(folderName.contains("\\"));
        assertEquals("a-b-c_JM" + AID, folderName);
    }

    /**
     * 构建出的名字必须能真正创建出本子文件夹+章节文件夹
     */
    @Test
    void folderNameIsCreatableOnCurrentOs() throws Exception {
        String folderName = service.buildAlbumFolderName(REAL_ALBUM_NAME, AID);
        File albumDir = new File(com.haruhi.botserver.shared.util.FileUtil.getAppTempDir(), folderName);
        File chapterDir = new File(albumDir, "第1话");
        try {
            assertTrue(chapterDir.mkdirs(), "创建章节文件夹失败：" + chapterDir.getAbsolutePath());
            File image = new File(chapterDir, "00001.webp.tmp");
            assertTrue(image.createNewFile(), "创建图片临时文件失败：" + image.getAbsolutePath());
            // windows 下非法名字在此处会抛 IOException("Bad pathname")
            assertTrue(image.getCanonicalPath().contains(folderName));
        } finally {
            deleteQuietly(albumDir);
        }
    }

    private void deleteQuietly(File file) {
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                deleteQuietly(child);
            }
        }
        file.delete();
    }

    private static class TestJmcomicService extends JmcomicService {
    }
}
