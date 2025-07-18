package com.haruhi.botServer.dto.qqclient;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

import java.io.Serializable;

/**
 * 群成员信息
 */
@Data
public class GroupMember implements Serializable {
    private int age;
    private String area;
    private String card;
    @JSONField(name = "card_changeable")
    private Boolean cardChangeable;
    @JSONField(name = "group_id")
    private Long groupId;
    @JSONField(name = "join_time")
    private Long joinTime;
    @JSONField(name = "last_sent_time")
    private Long lastSentTime;
    private String level;
    private String nickname;
    private String role;
    private String sex;
    @JSONField(name = "shut_up_timestamp")
    private Integer shutUpTimestamp;
    private String title;
    @JSONField(name = "title_expire_time")
    private Integer titleExpireTime;
    private Boolean unfriendly;
    @JSONField(name = "user_id")
    private Long userId;
}
