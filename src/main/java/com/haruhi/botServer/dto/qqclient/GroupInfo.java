package com.haruhi.botServer.dto.qqclient;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

/**
 * 群信息
 */
@Data
public class GroupInfo {
    @JSONField(name = "group_id")
    private String groupId;
    @JSONField(name = "group_name")
    private String groupName;
    @JSONField(name = "group_memo")
    private String groupMemo;
    @JSONField(name = "group_create_time")
    private Long groupCreateTime;
    @JSONField(name = "group_level")
    private Integer groupLevel;
    @JSONField(name = "member_count")
    private Integer memberCount;
    @JSONField(name = "max_member_count")
    private Integer maxMemberCount;
}
