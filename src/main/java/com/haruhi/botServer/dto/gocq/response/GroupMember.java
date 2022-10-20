package com.haruhi.botServer.dto.gocq.response;

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
    private boolean card_changeable;
    private String group_id;
    private long join_time;
    private long last_sent_time;
    private String level;
    private String nickname;
    private String role;
    private String sex;
    private int shut_up_timestamp;
    private String title;
    private int title_expire_time;
    private boolean unfriendly;
    private String user_id;
}
