package com.haruhi.botServer.vo;

import lombok.Data;

@Data
public class GroupChatUserResp {


    private Long userId;
    private String nickname;
    private String card;
    private Long count;

    private String time;

    private String userAvatarUrl;
}
