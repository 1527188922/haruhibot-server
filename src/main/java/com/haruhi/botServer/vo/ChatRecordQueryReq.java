package com.haruhi.botServer.vo;

import lombok.Data;

@Data
public class ChatRecordQueryReq extends PageReq {

    private String content;
    private String messageType;
    private Long userId;
    private Long groupId;
    private String nickName;
    private String card;
    private Long selfId;
    private Long chatId;

    // yyyy-MM-dd HH:mm:ss
    private String startTime;
    private String endTime;
    private String sort = "desc";
}
