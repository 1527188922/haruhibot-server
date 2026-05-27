package com.haruhi.botServer.vo;

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
