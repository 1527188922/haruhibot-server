package com.haruhi.botServer.utils;

import java.security.SecureRandom;

public class BilibililSidUtil {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public static String generate() {
        long millisecond = System.currentTimeMillis();
        String timeHex = longToHexUpper(millisecond);
        String random8Hex = generateRandom8Hex();
        return random8Hex + "_" + timeHex;
    }

    private static String longToHexUpper(long num) {
        return Long.toHexString(num).toUpperCase();
    }

    private static String generateRandom8Hex() {
        int random32 = SECURE_RANDOM.nextInt();
        return String.format("%08X", random32);
    }
}