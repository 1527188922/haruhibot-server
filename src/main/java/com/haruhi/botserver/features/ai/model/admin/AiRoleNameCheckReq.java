package com.haruhi.botserver.features.ai.model.admin;

import lombok.Data;

/**
 * 角色名可用性检查；sourceName 非空时返回复制建议名。
 */
@Data
public class AiRoleNameCheckReq {

    private String name;
    private String originalName;
    private String sourceName;
}
