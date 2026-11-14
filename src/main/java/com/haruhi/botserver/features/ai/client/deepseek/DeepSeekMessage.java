package com.haruhi.botserver.features.ai.client.deepseek;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DeepSeek Chat Completions 的一条消息。本期 content 只用纯文本。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeepSeekMessage {

    private String role;
    private String content;
    private String name;

    public static DeepSeekMessage system(String content) {
        return new DeepSeekMessage("system", content, null);
    }

    public static DeepSeekMessage user(String content) {
        return new DeepSeekMessage("user", content, null);
    }

    public static DeepSeekMessage assistant(String content) {
        return new DeepSeekMessage("assistant", content, null);
    }
}
