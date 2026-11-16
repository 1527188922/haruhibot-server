package com.haruhi.botserver.features.ai.model.admin;

import lombok.Data;

/**
 * WebUI 会话列表/详情头部。
 */
@Data
public class AiSessionView {

    private Long id;
    private Long selfId;
    private String scene;
    private Long targetId;
    /** 会话显式指定的角色，空表示走映射/全局默认 */
    private String roleName;
    private String status;
    private String lastActiveAt;
    private String createdAt;
    private long messageCount;
    private long todayTokens;
}
