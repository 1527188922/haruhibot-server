package com.haruhi.botserver.features.ai.model.admin;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * WebUI AI 概览。
 */
@Data
public class AiOverviewResp {

    private Usage today = new Usage();
    private Runtime runtime = new Runtime();
    private Balance balance = new Balance();

    @Data
    public static class Usage {
        private long requests;
        private long promptTokens;
        private long completionTokens;
        private long totalTokens;
        private long cacheHitTokens;
        private long failures;
        private double avgLatencyMs;
    }

    @Data
    public static class Runtime {
        private boolean enabled;
        private String model;
        private boolean thinking;
        private int groupWhitelistCount;
        private boolean privateEnabled;
        private int dailyTokenLimit;
        private int roleCount;
        private int emojiCount;
        private boolean searchEnabled;
        private boolean visionEnabled;
        private boolean agentEnabled;
    }

    @Data
    public static class Balance {
        private boolean available;
        private String error;
        private List<Info> items = new ArrayList<>();
    }

    @Data
    public static class Info {
        private String currency;
        private String totalBalance;
        private String grantedBalance;
        private String toppedUpBalance;
    }
}
