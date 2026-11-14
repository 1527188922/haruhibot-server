package com.haruhi.botserver.features.ai.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 一条进入 AI 链路的入站消息（已从 OneBot Message 提取成模型可读形式）。
 */
@Data
public class AiIncoming {

    private AiSessionKey key;
    private long userId;
    private String userName;
    private String text;
    private boolean atBot;
    private boolean atOnly;
    private boolean hasImage;
    private String messageId;
    private final List<String> messageIds = new ArrayList<>();
    private String replyMessageId;
    private long timestamp;
    private String rawMessage;

    /**
     * 合并同一用户连发时追加文本。
     */
    public void appendText(String more) {
        if (more == null || more.isBlank()) {
            return;
        }
        if (text == null || text.isBlank()) {
            text = more;
        } else {
            text = text + "\n" + more;
        }
    }
}
