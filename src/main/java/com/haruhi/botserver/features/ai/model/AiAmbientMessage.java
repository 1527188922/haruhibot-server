package com.haruhi.botserver.features.ai.model;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 群聊环境上下文中的一条消息。
 */
@Data
@AllArgsConstructor
public class AiAmbientMessage {

    private String messageId;
    private long speakerId;
    private String speakerName;
    private String text;
    private long timestamp;
}
