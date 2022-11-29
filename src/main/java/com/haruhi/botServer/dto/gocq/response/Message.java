package com.haruhi.botServer.dto.gocq.response;

import lombok.Data;

import java.io.Serializable;

@Data
public class Message implements Serializable {

    private String postType;
    private String metaEventType;
    private String messageType;
    private String noticeType;
    // 操作人id 比如群管理员a踢了一个人,那么该值为a的qq号
    private String operatorId;
    private Long time;
    private Long selfId;
    private String subType;
    private Long userId;
    private Long senderId;
    private Long groupId;
    private Long targetId;
    private String message;
    private String rawMessage;
    private Integer font;
    private Sender sender;
    private String messageId;
    private Integer messageSeq;
    private String anonymous;
}
