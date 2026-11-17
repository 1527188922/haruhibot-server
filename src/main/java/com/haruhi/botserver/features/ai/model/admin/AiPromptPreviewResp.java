package com.haruhi.botserver.features.ai.model.admin;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 提示词预览结果。
 */
@Data
public class AiPromptPreviewResp {

    private String system;
    private List<Section> sections = new ArrayList<>();
    private List<Message> messages = new ArrayList<>();
    private int estimatedTokens;
    private int budget;
    private boolean visionEnabled;

    @Data
    public static class Section {
        private String title;
        private String content;
    }

    @Data
    public static class Message {
        private String role;
        private String content;
        private String name;
        private List<String> imageUrls;
    }
}
