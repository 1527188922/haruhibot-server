package com.haruhi.botserver.features.ai.model.admin;

import lombok.Data;

/**
 * 角色名检查结果。
 */
@Data
public class AiRoleNameCheckResp {

    private boolean available;
    private String suggested;
    private String message;
}
