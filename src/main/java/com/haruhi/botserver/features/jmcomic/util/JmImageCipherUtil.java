package com.haruhi.botserver.features.jmcomic.util;

import com.haruhi.botserver.configuration.metadata.ConfigKey;
import com.haruhi.botserver.configuration.service.Configs;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.SequenceInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * JM图片本地加密工具。
 * <p>
 * 文件格式（16字节头 + 数据）：
 * <pre>
 * 0-3   magic "JMEN"
 * 4     version = 1
 * 5     flags，bit0=1 表示整图加密
 * 6-7   保留
 * 8-11  prefixLen，int32 大端，仅"部分加密"模式有效
 * 12-15 nonce，随机4字节，作为 AES-CTR 计数器的高4字节，低12字节固定为0
 * </pre>
 * 采用 AES/CTR/NoPadding：keystream = AES(counter)，counter 为完整16字节大端自增，
 * 保证与前端 webui/src/util/jm-image-crypto.js 的实现一致。
 * <p>
 * 加解密只取决于文件头，与当前配置无关，因此切换配置后历史明文/密文文件都能正常读取。
 */
public final class JmImageCipherUtil {

    private static final byte[] MAGIC = {'J', 'M', 'E', 'N'};
    private static final byte VERSION = 1;
    private static final int HEADER_LENGTH = 16;
    private static final int FLAG_FULL = 1;
    private static final int DEFAULT_PREFIX_KB = 32;

    /**
     * 固定密钥，需与前端保持完全一致（仅用于本地文件混淆，不作为强安全边界）
     */
    private static final byte[] KEY = "HaruhiJmImgKey01".getBytes(StandardCharsets.US_ASCII);
    private static final String TRANSFORMATION = "AES/CTR/NoPadding";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int COPY_BUFFER_SIZE = 8192;

    private JmImageCipherUtil() {
    }

    /**
     * 当前配置是否加密保存新下载的图片
     */
    public static boolean isEnabled() {
        return Configs.getBool(ConfigKey.JM_IMAGE_ENCRYPT_ENABLED, true);
    }

    /**
     * 当前配置的加密前缀长度（字节）。0 表示整图加密
     */
    public static int getPrefixBytes() {
        int kb = Configs.getInt(ConfigKey.JM_IMAGE_ENCRYPT_PREFIX_KB, DEFAULT_PREFIX_KB);
        if (kb < 0) {
            kb = DEFAULT_PREFIX_KB;
        }
        if (kb == 0) {
            return 0;
        }
        long bytes = (long) kb * 1024L;
        return bytes > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) bytes;
    }

    /**
     * 判断文件是否为加密后的JM图片（只看文件头魔数，旧版明文图片返回false）
     */
    public static boolean isEncrypted(File file) {
        if (file == null || !file.isFile() || file.length() < HEADER_LENGTH) {
            return false;
        }
        try (InputStream in = new FileInputStream(file)) {
            byte[] magic = in.readNBytes(4);
            return magic.length == 4 && isMagic(magic, 0);
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * 按当前配置创建加密输出流。
     * <p>
     * prefixKb=0 时为整图加密，无需知道文件大小；大于0时先缓存前 N KB，
     * 若文件不足 N KB 则按实际长度整段加密。
     */
    public static OutputStream newEncryptingStream(File target) throws IOException {
        return newEncryptingStream(target, getPrefixBytes());
    }

    public static OutputStream newEncryptingStream(File target, int prefixBytes) throws IOException {
        byte[] nonce = new byte[4];
        RANDOM.nextBytes(nonce);
        FileOutputStream out = new FileOutputStream(target);
        if (prefixBytes <= 0) {
            try {
                out.write(buildHeader((byte) FLAG_FULL, 0, nonce));
                return new CipherOutputStream(out, newCipher(Cipher.ENCRYPT_MODE, nonce));
            } catch (IOException e) {
                closeQuietly(out);
                throw e;
            }
        }
        return new PrefixEncryptingOutputStream(out, nonce, prefixBytes);
    }

    /**
     * 把一个明文文件加密写入目标文件（用于封面等已经落盘的文件）
     */
    public static void encryptFile(File source, File target) throws IOException {
        try (InputStream in = new BufferedInputStream(new FileInputStream(source));
             OutputStream out = newEncryptingStream(target)) {
            in.transferTo(out);
        } catch (IOException e) {
            Files.deleteIfExists(target.toPath());
            throw e;
        }
    }

    /**
     * 创建自动识别明文的解密输入流。
     * <p>
     * 文件头不是JM加密格式时按明文原样返回（兼容关闭开关前后历史文件）。
     */
    public static InputStream newDecryptingStream(File file) throws IOException {
        FileInputStream in = new FileInputStream(file);
        try {
            byte[] header = in.readNBytes(HEADER_LENGTH);
            if (header.length < HEADER_LENGTH || !isMagic(header, 0)) {
                // 明文图片：把已经读出的少量字节拼回
                return new SequenceInputStream(new ByteArrayInputStream(header), in);
            }
            byte flags = header[5];
            boolean fullEncrypted = (flags & FLAG_FULL) != 0;
            int prefixLength = readInt(header, 8);
            byte[] nonce = Arrays.copyOfRange(header, 12, 16);
            if (fullEncrypted) {
                return new CipherInputStream(in, newCipher(Cipher.DECRYPT_MODE, nonce));
            }
            if (prefixLength < 0) {
                prefixLength = 0;
            }
            return new PrefixDecryptingInputStream(in, newCipher(Cipher.DECRYPT_MODE, nonce), prefixLength);
        } catch (IOException e) {
            closeQuietly(in);
            throw e;
        }
    }

    private static boolean isMagic(byte[] bytes, int offset) {
        return bytes[offset] == MAGIC[0] && bytes[offset + 1] == MAGIC[1]
                && bytes[offset + 2] == MAGIC[2] && bytes[offset + 3] == MAGIC[3];
    }

    private static byte[] buildHeader(byte flags, int prefixLength, byte[] nonce) {
        byte[] header = new byte[HEADER_LENGTH];
        System.arraycopy(MAGIC, 0, header, 0, MAGIC.length);
        header[4] = VERSION;
        header[5] = flags;
        writeInt(header, 8, prefixLength);
        System.arraycopy(nonce, 0, header, 12, nonce.length);
        return header;
    }

    private static Cipher newCipher(int mode, byte[] nonce) throws IOException {
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            byte[] iv = new byte[16];
            System.arraycopy(nonce, 0, iv, 0, nonce.length);
            cipher.init(mode, new SecretKeySpec(KEY, "AES"), new IvParameterSpec(iv));
            return cipher;
        } catch (Exception e) {
            throw new IOException("初始化JM图片加解密失败", e);
        }
    }

    private static int readInt(byte[] bytes, int offset) {
        return ((bytes[offset] & 0xFF) << 24)
                | ((bytes[offset + 1] & 0xFF) << 16)
                | ((bytes[offset + 2] & 0xFF) << 8)
                | (bytes[offset + 3] & 0xFF);
    }

    private static void writeInt(byte[] bytes, int offset, int value) {
        bytes[offset] = (byte) (value >>> 24);
        bytes[offset + 1] = (byte) (value >>> 16);
        bytes[offset + 2] = (byte) (value >>> 8);
        bytes[offset + 3] = (byte) value;
    }

    private static void closeQuietly(AutoCloseable closeable) {
        try {
            closeable.close();
        } catch (Exception ignored) {
        }
    }

    /**
     * 部分加密输出流：先缓存前 prefixBytes 字节，凑满后再写文件头并把这段密文写出去，
     * 之后的字节保持明文直写
     */
    private static final class PrefixEncryptingOutputStream extends OutputStream {
        private final FileOutputStream out;
        private final byte[] nonce;
        private final int prefixBytes;
        private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        private CipherOutputStream cipherOut;
        private boolean closed;

        private PrefixEncryptingOutputStream(FileOutputStream out, byte[] nonce, int prefixBytes) {
            this.out = out;
            this.nonce = nonce;
            this.prefixBytes = prefixBytes;
        }

        @Override
        public void write(int b) throws IOException {
            if (cipherOut != null) {
                cipherOut.write(b);
                return;
            }
            buffer.write(b);
            if (buffer.size() >= prefixBytes) {
                flushPrefix();
            }
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            if (cipherOut != null) {
                cipherOut.write(b, off, len);
                return;
            }
            int take = Math.min(prefixBytes - buffer.size(), len);
            buffer.write(b, off, take);
            if (buffer.size() >= prefixBytes) {
                flushPrefix();
            }
            int remain = len - take;
            if (remain > 0) {
                cipherOut.write(b, off + take, remain);
            }
        }

        private void flushPrefix() throws IOException {
            byte[] plainPrefix = buffer.toByteArray();
            out.write(buildHeader((byte) 0, plainPrefix.length, nonce));
            cipherOut = new CipherOutputStream(out, newCipher(Cipher.ENCRYPT_MODE, nonce));
            cipherOut.write(plainPrefix);
            buffer.reset();
        }

        @Override
        public void flush() throws IOException {
            if (cipherOut != null) {
                cipherOut.flush();
            } else {
                out.flush();
            }
        }

        @Override
        public void close() throws IOException {
            if (closed) {
                return;
            }
            closed = true;
            try {
                if (cipherOut == null) {
                    // 文件不足 prefixBytes（或空文件）：按实际长度整段加密
                    flushPrefix();
                }
                cipherOut.close();
            } catch (IOException e) {
                closeQuietly(out);
                throw e;
            }
        }
    }

    /**
     * 部分解密输入流：前 prefixLength 字节经过 Cipher 解密，其余字节明文透传
     */
    private static final class PrefixDecryptingInputStream extends InputStream {
        private final InputStream raw;
        private final Cipher cipher;
        private final byte[] singleByte = new byte[1];
        private long prefixRemaining;

        private PrefixDecryptingInputStream(InputStream raw, Cipher cipher, long prefixLength) {
            this.raw = raw;
            this.cipher = cipher;
            this.prefixRemaining = prefixLength;
        }

        @Override
        public int read() throws IOException {
            int read = read(singleByte, 0, 1);
            return read == -1 ? -1 : (singleByte[0] & 0xFF);
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (len == 0) {
                return 0;
            }
            if (prefixRemaining <= 0) {
                return raw.read(b, off, len);
            }
            int toRead = (int) Math.min(len, prefixRemaining);
            int read = raw.read(b, off, toRead);
            if (read <= 0) {
                return read;
            }
            int decrypted;
            try {
                decrypted = cipher.update(b, off, read, b, off);
            } catch (GeneralSecurityException e) {
                throw new IOException("解密JM图片失败", e);
            }
            prefixRemaining -= decrypted;
            return decrypted;
        }

        @Override
        public void close() throws IOException {
            raw.close();
        }
    }
}
