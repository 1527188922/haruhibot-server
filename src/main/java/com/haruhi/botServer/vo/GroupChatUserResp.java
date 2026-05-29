package com.haruhi.botServer.vo;

import com.haruhi.botServer.entity.vo.AvatarInfo;
import lombok.Data;

@Data
public class GroupChatUserResp extends AvatarInfo {

    private Long id;
    private Long userId;
    private String nickname;
    private String card;
    private Long count;

    private String time;

}
