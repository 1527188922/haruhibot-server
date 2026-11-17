package com.haruhi.botserver.features.ai.model.admin;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 角色卡保存/新建/预览请求。
 */
@Data
public class AiRoleSaveReq {

    /** 为空表示新建 */
    private String originalName;
    private String name;
    private List<String> triggers = new ArrayList<>();
    private List<AiRoleSection> sections = new ArrayList<>();
    /** 打开编辑器时的文件 mtime，用于冲突检测 */
    private long baseMtime;
    /** 打开编辑器时的文件 SHA-256，优先于 mtime */
    private String baseHash;
    private boolean dryRun;
}
