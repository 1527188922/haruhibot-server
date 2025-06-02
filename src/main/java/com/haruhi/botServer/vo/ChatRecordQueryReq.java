package com.haruhi.botServer.vo;

import lombok.Data;

@Data
public class ChatRecordQueryReq extends PageReq {

    private String content;
    private String messageType;
    private Long userId;
    private Long groupId;
}
