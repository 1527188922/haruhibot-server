package com.haruhi.botserver.features.ai.model.admin;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 按天用量趋势。
 */
@Data
public class AiUsageTrendResp {

    private List<Point> points = new ArrayList<>();

    @Data
    public static class Point {
        private String date;
        private long requests;
        private long promptTokens;
        private long completionTokens;
        private long totalTokens;
        private long cacheHitTokens;
        private long failures;
        private double avgLatencyMs;
    }
}
