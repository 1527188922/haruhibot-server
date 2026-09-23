package com.haruhi.botserver.shared.util;

import java.security.SecureRandom;
import java.util.Base64;

public final class SecretGenerator {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();

    private SecretGenerator() {
    }

    /**
     * 默认生成 48 字节，即 384 位，Base64 URL 安全编码后 64 个字符
     */
    public static String generateSecret() {
        return generateSecret(48);
    }

    /**
     * @param byteLength 至少 32 字节，即 256 位，HS256 最低要求
     */
    public static String generateSecret(int byteLength) {
        if (byteLength < 32) {
            throw new IllegalArgumentException("JWT secret 至少需要 32 字节（256 位）");
        }
        byte[] bytes = new byte[byteLength];
        SECURE_RANDOM.nextBytes(bytes);
        return URL_ENCODER.encodeToString(bytes);
    }

    public static void main(String[] args) {
        String secret = generateSecret();
        System.out.println("login.jwt.secret=" + secret);
    }
}