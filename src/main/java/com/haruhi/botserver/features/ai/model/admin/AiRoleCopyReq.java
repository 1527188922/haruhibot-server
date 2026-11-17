package com.haruhi.botserver.features.ai.model.admin;

import lombok.Data;

/**
 * 角色卡复制请求。
 */
@Data
public class AiRoleCopyReq {

    private String sourceName;
    private String targetName;
    private boolean overwrite;
}
