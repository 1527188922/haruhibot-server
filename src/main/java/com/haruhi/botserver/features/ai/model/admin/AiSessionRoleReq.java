package com.haruhi.botserver.features.ai.model.admin;

import lombok.Data;

/**
 * 会话切换角色请求；roleName 为空表示恢复默认解析。
 */
@Data
public class AiSessionRoleReq {

    private Long id;
    private String roleName;
}
