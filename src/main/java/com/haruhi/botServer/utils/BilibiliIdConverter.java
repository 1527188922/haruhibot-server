package com.haruhi.botServer.utils;

import java.util.HashMap;
import java.util.Map;

public class BilibiliIdConverter {
    private static final long XOR_CODE = 23442827791579L;
    private static final long MASK_CODE = 2251799813685247L;
    private static final long MAX_AID = 1L << 51;
    private static final int BASE = 58;
    private static final int BV_LEN = 12;
    private static final String PREFIX = "BV1";
    
    private static final char[] DATA = {
        'F', 'c', 'w', 'A', 'P', 'N', 'K', 'T', 'M', 'u', 'g', '3',
        'G', 'V', '5', 'L', 'j', '7', 'E', 'J', 'n', 'H', 'p', 'W',
        's', 'x', '4', 't', 'b', '8', 'h', 'a', 'Y', 'e', 'v', 'i',
        'q', 'B', 'z', '6', 'r', 'k', 'C', 'y', '1', '2', 'm', 'U',
        'S', 'D', 'Q', 'X', '9', 'R', 'd', 'o', 'Z', 'f'
    };
    
    private static final Map<Character, Integer> DATA_INDEX_MAP = new HashMap<>();
    static {
        for (int i = 0; i < DATA.length; i++) {
            DATA_INDEX_MAP.put(DATA[i], i);
        }
    }

    public static long bvid2aid(String bvid) {
        if (bvid == null || bvid.length() != BV_LEN || !bvid.startsWith(PREFIX)) {
            throw new IllegalArgumentException("Invalid BV format");
        }
        
        char[] chars = bvid.toCharArray();
        // Swap positions 3 and 9
        swap(chars, 3, 9);
        // Swap positions 4 and 7
        swap(chars, 4, 7);
        
        long tmp = 0;
        for (int i = 3; i < BV_LEN; i++) {
            char c = chars[i];
            Integer index = DATA_INDEX_MAP.get(c);
            if (index == null) {
                throw new IllegalArgumentException("Invalid character in BV: " + c);
            }
            tmp = tmp * BASE + index;
        }
        return (tmp & MASK_CODE) ^ XOR_CODE;
    }

    public static String aid2bvid(long aid) {
        if (aid < 0 || aid > MAX_AID) {
            throw new IllegalArgumentException("Invalid AV number");
        }
        
        char[] bytes = new char[BV_LEN];
        bytes[0] = 'B';
        bytes[1] = 'V';
        bytes[2] = '1';
        for (int i = 3; i < BV_LEN; i++) {
            bytes[i] = '0';
        }
        
        int bvIdx = BV_LEN - 1;
        long tmp = (MAX_AID | aid) ^ XOR_CODE;
        
        while (tmp != 0) {
            bytes[bvIdx--] = DATA[(int)(tmp % BASE)];
            tmp /= BASE;
        }
        
        // Swap positions 3 and 9
        swap(bytes, 3, 9);
        // Swap positions 4 and 7
        swap(bytes, 4, 7);
        
        return new String(bytes);
    }

    private static void swap(char[] arr, int i, int j) {
        char temp = arr[i];
        arr[i] = arr[j];
        arr[j] = temp;
    }

    public static void main(String[] args) {
        String bvid = "BV1EW411y7Gq";
        long aid = bvid2aid(bvid);
        System.out.println(bvid + " -> AV" + aid);
        
        String newBvid = aid2bvid(31708503L);
        System.out.println("AV31708503 -> " + newBvid);
    }
}