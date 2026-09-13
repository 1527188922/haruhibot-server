package com.haruhi.botServer.vo;

import lombok.Data;

@Data
public class GroupMemberQueryReq extends PageReq {

    private Long selfId;
    private Long groupId;
    private Long userId;
    private String nickname;
    private String card;
    /**
     * 是否离群  null:全部 true:已离群 false:在群
     */
    private Boolean leftFlag;
}
