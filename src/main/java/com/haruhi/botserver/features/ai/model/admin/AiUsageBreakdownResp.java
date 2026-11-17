package com.haruhi.botserver.features.ai.model.admin;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 用量分布：场景 / 模型 / 群。
 */
@Data
public class AiUsageBreakdownResp {

    private List<Item> byScene = new ArrayList<>();
    private List<Item> byModel = new ArrayList<>();
    private List<Item> byGroup = new ArrayList<>();

    @Data
    public static class Item {
        private String key;
        private String label;
        private long requests;
        private long tokens;
        private long failures;
    }
}
