package com.haruhi.botserver.features.contacts.model;

import com.haruhi.botserver.shared.model.PageReq;

import lombok.Data;

@Data
public class GroupMemberQueryReq extends PageReq {

    private Long selfId;
    private Long groupId;
    private Long userId;
    private String nickname;
    private String card;
    /**
     * 是否离群  null:全部 1:已离群 0:在群
     */
    private Integer leftFlag;
}
