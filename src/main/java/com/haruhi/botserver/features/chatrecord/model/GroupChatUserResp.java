package com.haruhi.botserver.features.chatrecord.model;

import com.haruhi.botserver.features.contacts.model.AvatarInfo;
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
