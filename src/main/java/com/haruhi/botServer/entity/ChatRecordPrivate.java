package com.haruhi.botServer.entity;

import lombok.Data;

@Data
public class ChatRecordPrivate {

    private Long id;
    private String nickname;
    private String messageId;
    private Long userId;
    private String content;
    private Integer deleted;
    private String time;
}
