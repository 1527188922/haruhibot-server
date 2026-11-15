package com.haruhi.botserver.features.ai.client.deepseek;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DeepSeek Chat Completions 的一条消息。content 为纯文本，imageUrls 非空时按多模态 content 数组发送。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeepSeekMessage {

    private String role;
    private String content;
    private String name;
    private List<String> imageUrls;

    public static DeepSeekMessage system(String content) {
        return new DeepSeekMessage("system", content, null, null);
    }

    public static DeepSeekMessage user(String content) {
        return new DeepSeekMessage("user", content, null, null);
    }

    public static DeepSeekMessage userWithImages(String content, List<String> imageUrls) {
        return new DeepSeekMessage("user", content, null, imageUrls);
    }

    public static DeepSeekMessage assistant(String content) {
        return new DeepSeekMessage("assistant", content, null, null);
    }
}
