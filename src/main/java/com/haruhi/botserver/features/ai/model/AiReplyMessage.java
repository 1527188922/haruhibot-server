package com.haruhi.botserver.features.ai.model;

import lombok.Data;

/**
 * 模型输出的一条待发送消息。
 */
@Data
public class AiReplyMessage {

    private String text;
    private int delayMs;
    private String replyTo;
    private String at;
}
