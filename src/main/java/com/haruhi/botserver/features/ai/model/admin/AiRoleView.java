package com.haruhi.botserver.features.ai.model.admin;

import lombok.Data;

import java.util.List;

/**
 * WebUI 角色卡列表项。
 */
@Data
public class AiRoleView {

    private String name;
    private String fileName;
    private long chars;
    private long updatedAt;
    private String updatedAtText;
    private List<String> triggerWords;
    private List<String> warnings;
    /** 显式指定该角色的会话数 */
    private long usedBySessions;
}
