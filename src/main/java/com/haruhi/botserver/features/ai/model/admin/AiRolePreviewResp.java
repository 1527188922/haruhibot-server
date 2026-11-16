package com.haruhi.botserver.features.ai.model.admin;

import lombok.Data;

import java.util.List;

/**
 * WebUI 角色卡预览。
 */
@Data
public class AiRolePreviewResp {

    private String name;
    private String fileName;
    private long chars;
    private String updatedAtText;
    private List<String> triggerWords;
    private List<String> warnings;
    /** 按当前工具/视觉开关过滤后的内容 */
    private String content;
    private int estimatedTokens;
    private boolean toolsEnabled;
    private boolean visionEnabled;
}
