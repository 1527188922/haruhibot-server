package com.haruhi.botServer.entity.vo;

import lombok.Data;

@Data
public class ChatRecordVo extends AvatarInfo{

    private Long id;
    private String card;
    private String nickname;
    private String messageId;
    private Long groupId;
    private Long userId;
    private Long targetId;//私聊使用
    private Long selfId;
    private String content;
    private String time;
    private Integer deleted;

    private String messageType;

    private Long total;

    private String groupName;

}
