package com.haruhi.botserver.features.ai.model;

import lombok.Data;

/**
 * 群聊环境上下文中的一条消息。
 */
@Data
public class AiAmbientMessage {

    private String messageId;
    private long speakerId;
    private String speakerName;
    private String text;
    private long timestamp;
    /** 这条消息回复的目标消息 ID，用于让模型看到“谁在回谁” */
    private String replyToMessageId;

    public AiAmbientMessage(String messageId, long speakerId, String speakerName, String text, long timestamp) {
        this(messageId, speakerId, speakerName, text, timestamp, null);
    }

    public AiAmbientMessage(String messageId, long speakerId, String speakerName, String text, long timestamp,
                            String replyToMessageId) {
        this.messageId = messageId;
        this.speakerId = speakerId;
        this.speakerName = speakerName;
        this.text = text;
        this.timestamp = timestamp;
        this.replyToMessageId = replyToMessageId;
    }
}
