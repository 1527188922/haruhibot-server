package com.haruhi.botserver.features.contacts.model;

import com.haruhi.botserver.shared.model.PageReq;

import lombok.Data;

@Data
public class FriendInfoQueryReq extends PageReq{

    private Long selfId;
    /**
     * 好友用户ID
     */
    private Long userId;

    /**
     * 性别
     */
    private String sex;

    /**
     * 昵称
     */
    private String nickname;
}
