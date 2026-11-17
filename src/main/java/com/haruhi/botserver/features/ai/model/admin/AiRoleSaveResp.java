package com.haruhi.botserver.features.ai.model.admin;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 角色卡保存结果。
 */
@Data
public class AiRoleSaveResp {

    private boolean saved;
    private AiRoleDetailResp detail;
    private String renderedMarkdown;
    private String filteredContent;
    private List<String> warnings = new ArrayList<>();
    private int estimatedTokens;
}
