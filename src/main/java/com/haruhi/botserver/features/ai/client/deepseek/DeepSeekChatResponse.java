package com.haruhi.botserver.features.ai.client.deepseek;

import lombok.Data;

@Data
public class DeepSeekChatResponse {

    private String id;
    private String model;
    private String content;
    private String reasoningContent;
    private String finishReason;
    private DeepSeekUsage usage;
    private String raw;
}
