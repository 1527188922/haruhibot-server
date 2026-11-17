package com.haruhi.botserver.features.ai.model.admin;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 角色卡结构化编辑数据。
 */
@Data
public class AiRoleDetailResp {

    private String name;
    private String fileName;
    private List<String> triggers = new ArrayList<>();
    private List<AiRoleSection> sections = new ArrayList<>();
    /** 按当前开关过滤后的内容字符数 */
    private int chars;
    private int maxChars;
    private long mtime;
    private String hash;
    private List<String> warnings = new ArrayList<>();
}
