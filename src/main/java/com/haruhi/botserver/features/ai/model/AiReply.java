package com.haruhi.botserver.features.ai.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 模型一次回复的协议解析结果。
 */
@Data
public class AiReply {

    public enum Type {
        SAY, CALL, SILENT, INVALID
    }

    private Type type = Type.INVALID;
    private List<AiReplyMessage> messages = new ArrayList<>();
    private AiToolCall call;
    private String rawContent;
    private String error;

    public boolean isSay() {
        return type == Type.SAY;
    }

    public boolean isCall() {
        return type == Type.CALL;
    }

    public boolean isSilent() {
        return type == Type.SILENT;
    }

    public boolean isInvalid() {
        return type == Type.INVALID;
    }

    public static AiReply say(List<AiReplyMessage> messages) {
        AiReply reply = new AiReply();
        reply.type = Type.SAY;
        reply.messages = messages;
        return reply;
    }

    public static AiReply call(AiToolCall call) {
        AiReply reply = new AiReply();
        reply.type = Type.CALL;
        reply.call = call;
        return reply;
    }

    public static AiReply silent() {
        AiReply reply = new AiReply();
        reply.type = Type.SILENT;
        return reply;
    }

    public static AiReply invalid(String error) {
        AiReply reply = new AiReply();
        reply.type = Type.INVALID;
        reply.error = error;
        return reply;
    }
}
