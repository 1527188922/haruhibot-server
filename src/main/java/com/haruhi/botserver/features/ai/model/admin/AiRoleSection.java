package com.haruhi.botserver.features.ai.model.admin;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 角色卡章节：标题 + 若干行。
 */
@Data
public class AiRoleSection {

    private String title;
    /** base / tools / vision */
    private String mode = "base";
    private List<String> lines = new ArrayList<>();
}
