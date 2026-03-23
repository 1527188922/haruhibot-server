package com.haruhi.botServer.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * 文本压缩/解压缩工具类
 */
public class TextCompressionUtils {

    public static byte[] compress(String originalText) throws IOException {
        if (originalText == null) {
            return new byte[0];
        }
        byte[] textBytes = originalText.getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (GZIPOutputStream gzipOut = new GZIPOutputStream(bos)) {
            gzipOut.write(textBytes);
        }
        return bos.toByteArray();
    }

    public static String decompress(byte[] compressedBytes) throws IOException {
        ByteArrayInputStream bis = new ByteArrayInputStream(compressedBytes);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (GZIPInputStream gzipIn = new GZIPInputStream(bis)) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = gzipIn.read(buffer)) != -1) {
                bos.write(buffer, 0, len);
            }
        }
        return bos.toString(StandardCharsets.UTF_8);
    }

}