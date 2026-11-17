package com.haruhi.botserver.features.ai.model.admin;

import lombok.Data;

/**
 * 用量统计时间范围，单位天。
 */
@Data
public class AiUsageRangeReq {

    private Integer days;
}
