package com.haruhi.botserver.features.jmcomic.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link JmImageCipherUtil} 的文件格式与兼容性测试。
 *
 * <p>锁三件事：
 * <ol>
 *     <li>整图加密往返一致；</li>
 *     <li>部分加密只加密前 N 字节，其余字节是明文（"前 32KB 正常、其余像素全乱"的回归点）；</li>
 *     <li>历史遗留文件（头部声明部分加密、实际整段都是密文）仍能正确解密，老图片不用重新下载。</li>
 * </ol>
 */
class JmImageCipherUtilTest {

    /** 固定密钥，与前后端实现保持一致；只在构造"历史坏文件"时需要。 */
    private static final byte[] KEY = "HaruhiJmImgKey01".getBytes(StandardCharsets.US_ASCII);
    private static final int PREFIX_BYTES = 32 * 1024;

    @TempDir
    Path tempDir;

    @Test
    void fullEncryptionRoundTrip() throws Exception {
        byte[] plain = randomBytes(200_000, 1);
        Path file = tempDir.resolve("full.webp");

        try (OutputStream out = JmImageCipherUtil.newEncryptingStream(file.toFile(), 0)) {
            out.write(plain);
        }

        assertTrue(JmImageCipherUtil.isEncrypted(file.toFile()), "整图加密文件应被识别为加密文件");
        byte[] all = Files.readAllBytes(file);
        assertEquals(plain.length + 16, all.length, "整图加密 = 16字节头 + 原长度");
        assertEquals(1, all[5] & 1, "flags bit0 表示整图加密");
        assertArrayEquals(plain, readAll(file), "整图加密往返应完全一致");
    }

    /**
     * 回归：部分加密必须只加密前缀，剩余字节保持明文。
     *
     * <p>首版把剩余字节也送进了 cipher，头部却仍写"部分加密"，于是前端只解前 32KB，
     * 剩下的密文被当成明文解码——用户看到的就是「前 32KB 正常、其他部分像素全乱」。
     */
    @Test
    void prefixEncryptionKeepsTailPlaintext() throws Exception {
        byte[] plain = randomBytes(200_000, 2);
        Path file = tempDir.resolve("prefix.webp");

        try (OutputStream out = JmImageCipherUtil.newEncryptingStream(file.toFile(), PREFIX_BYTES)) {
            out.write(plain);
        }

        byte[] all = Files.readAllBytes(file);
        assertEquals(16 + plain.length, all.length, "部分加密不改变文件长度");
        assertEquals(2, all[5] & 2, "flags bit1 表示只有前缀被加密");
        assertEquals(PREFIX_BYTES, readIntBE(all, 8), "头部 prefixLen = 实际加密字节数");

        byte[] prefixInFile = Arrays.copyOfRange(all, 16, 16 + PREFIX_BYTES);
        assertFalse(Arrays.equals(Arrays.copyOfRange(plain, 0, PREFIX_BYTES), prefixInFile),
                "前缀必须是密文");
        assertArrayEquals(Arrays.copyOfRange(plain, PREFIX_BYTES, plain.length),
                Arrays.copyOfRange(all, 16 + PREFIX_BYTES, all.length),
                "前缀之后的字节必须原样（明文），否则读取方会把它们当明文渲染成乱码");

        assertArrayEquals(plain, readAll(file), "部分加密往返应完全一致");
    }

    /**
     * 历史文件兼容：头部 flags=0（首版的"部分加密"）但整段都是密文。
     *
     * <p>这类文件不能只解前缀，否则后半段全是乱码；必须整段解密。
     */
    @Test
    void legacyPartialHeaderDecryptsWholeBody() throws Exception {
        byte[] plain = randomBytes(120_000, 3);
        Path file = tempDir.resolve("legacy.webp");
        Files.write(file, buildLegacyFile(plain, PREFIX_BYTES));

        assertArrayEquals(plain, readAll(file), "历史坏文件必须按整段密文解密");
    }

    @Test
    void plaintextFilePassesThrough() throws Exception {
        byte[] plain = randomBytes(4096, 4);
        // 伪造一个 WebP 头，明确不是 JMEN
        plain[0] = 'R';
        plain[1] = 'I';
        plain[2] = 'F';
        plain[3] = 'F';
        Path file = tempDir.resolve("plain.webp");
        Files.write(file, plain);

        assertFalse(JmImageCipherUtil.isEncrypted(file.toFile()), "明文文件不应被识别为加密文件");
        assertArrayEquals(plain, readAll(file), "明文文件应原样返回");
    }

    /** 文件比配置的前缀还短：整段当前缀加密，prefixLen 记实际长度。 */
    @Test
    void shortFileEncryptsWholeContentAsPrefix() throws Exception {
        byte[] plain = randomBytes(1000, 5);
        Path file = tempDir.resolve("short.webp");

        try (OutputStream out = JmImageCipherUtil.newEncryptingStream(file.toFile(), PREFIX_BYTES)) {
            out.write(plain);
        }

        byte[] all = Files.readAllBytes(file);
        assertEquals(16 + plain.length, all.length);
        assertEquals(2, all[5] & 2, "短文件同样是「前缀加密」格式");
        assertEquals(plain.length, readIntBE(all, 8), "prefixLen 记实际长度");
        assertArrayEquals(plain, readAll(file), "短文件往返应完全一致");
    }

    /** 逐字节写入（write(int)）走的是同一条前缀路径。 */
    @Test
    void singleByteWritesAlsoKeepTailPlaintext() throws Exception {
        byte[] plain = randomBytes(40_000, 6);
        Path file = tempDir.resolve("byteByByte.webp");

        try (OutputStream out = JmImageCipherUtil.newEncryptingStream(file.toFile(), PREFIX_BYTES)) {
            for (byte b : plain) {
                out.write(b);
            }
        }

        assertArrayEquals(plain, readAll(file), "逐字节写入的结果应与批量写入一致");
        byte[] all = Files.readAllBytes(file);
        assertArrayEquals(Arrays.copyOfRange(plain, PREFIX_BYTES, plain.length),
                Arrays.copyOfRange(all, 16 + PREFIX_BYTES, all.length),
                "逐字节写入时前缀之后也必须是明文");
    }

    private static byte[] readAll(Path file) throws Exception {
        try (InputStream in = JmImageCipherUtil.newDecryptingStream(file.toFile())) {
            assertNotNull(in);
            return in.readAllBytes();
        }
    }

    /**
     * 构造首版写坏的文件：头部 flags=0 + prefixLen=N，正文却是整段 AES-CTR 密文。
     */
    private static byte[] buildLegacyFile(byte[] plain, int prefixLength) throws Exception {
        byte[] nonce = {0x11, 0x22, 0x33, 0x44};
        byte[] header = new byte[16];
        header[0] = 'J';
        header[1] = 'M';
        header[2] = 'E';
        header[3] = 'N';
        header[4] = 1;
        header[5] = 0;
        writeIntBE(header, 8, prefixLength);
        System.arraycopy(nonce, 0, header, 12, nonce.length);

        Cipher cipher = Cipher.getInstance("AES/CTR/NoPadding");
        byte[] iv = new byte[16];
        System.arraycopy(nonce, 0, iv, 0, nonce.length);
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(KEY, "AES"), new IvParameterSpec(iv));
        byte[] cipherText = cipher.doFinal(plain);

        byte[] file = new byte[header.length + cipherText.length];
        System.arraycopy(header, 0, file, 0, header.length);
        System.arraycopy(cipherText, 0, file, header.length, cipherText.length);
        return file;
    }

    private static void writeIntBE(byte[] bytes, int offset, int value) {
        bytes[offset] = (byte) (value >>> 24);
        bytes[offset + 1] = (byte) (value >>> 16);
        bytes[offset + 2] = (byte) (value >>> 8);
        bytes[offset + 3] = (byte) value;
    }

    private static int readIntBE(byte[] bytes, int offset) {
        return ((bytes[offset] & 0xFF) << 24)
                | ((bytes[offset + 1] & 0xFF) << 16)
                | ((bytes[offset + 2] & 0xFF) << 8)
                | (bytes[offset + 3] & 0xFF);
    }

    private static byte[] randomBytes(int length, long seed) {
        byte[] bytes = new byte[length];
        new Random(seed).nextBytes(bytes);
        return bytes;
    }
}
