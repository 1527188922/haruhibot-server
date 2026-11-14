package com.haruhi.botserver.features.ai.model;

/**
 * AI 会话场景。
 */
public enum AiScene {
    GROUP("group"),
    PRIVATE("private");

    private final String code;

    AiScene(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static AiScene of(String code) {
        for (AiScene value : values()) {
            if (value.code.equalsIgnoreCase(code)) {
                return value;
            }
        }
        return null;
    }
}
