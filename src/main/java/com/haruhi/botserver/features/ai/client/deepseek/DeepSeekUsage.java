package com.haruhi.botserver.features.ai.client.deepseek;

import lombok.Data;

@Data
public class DeepSeekUsage {

    private int promptTokens;
    private int completionTokens;
    private int totalTokens;
    private int promptCacheHitTokens;
    private int promptCacheMissTokens;
}
