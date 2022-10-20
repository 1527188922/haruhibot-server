package com.haruhi.botServer.dto.gocq.response;

import lombok.Data;

/**
 * 群信息
 */
@Data
public class GroupInfo {
    private String group_id;
    private String group_name;
    private String group_memo;
    private Long group_create_time;
    private Integer group_level;
    private Integer member_count;
    private Integer max_member_count;
}
