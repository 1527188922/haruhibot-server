package com.haruhi.botServer.dto.qqclient;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GroupAtAllRemainInfo {

    // 是否可以艾特全体
    @JSONField(name = "can_at_all")
    private Boolean canAtAll;

    // 群艾特全体剩余次数
    @JSONField(name = "remain_at_all_count_for_group")
    private Integer remainAtAllCountForGroup;

    // 个人艾特全体剩余次数
    @JSONField(name = "remain_at_all_count_for_uin")
    private Integer remainAtAllCountForUin;

}
