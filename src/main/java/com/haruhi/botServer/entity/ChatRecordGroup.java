package com.haruhi.botServer.entity;

import lombok.Data;

@Data
public class ChatRecordGroup {

    private Long id;
    private String card;
    private String nickname;
    private String messageId;
    private Long userId;
    private Long selfId;
    private String content;
    private Integer deleted;
    private String time;

}
