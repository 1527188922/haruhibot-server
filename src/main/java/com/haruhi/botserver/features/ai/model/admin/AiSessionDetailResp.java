package com.haruhi.botserver.features.ai.model.admin;

import lombok.Data;

/**
 * WebUI 会话详情统计。
 */
@Data
public class AiSessionDetailResp {

    private AiSessionView session;
    /** 当前实际生效的角色卡名称，空表示无角色 */
    private String effectiveRole;
    private long totalRequests;
    private long totalTokens;
    private long failures;
    private double avgLatencyMs;
    private long memoryCount;
}
