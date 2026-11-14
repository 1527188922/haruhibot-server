package com.haruhi.botserver.features.ai.client.deepseek;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class DeepSeekBalanceResponse {

    private boolean available;
    private List<BalanceInfo> balances = new ArrayList<>();

    @Data
    public static class BalanceInfo {
        private String currency;
        private String totalBalance;
        private String grantedBalance;
        private String toppedUpBalance;
    }
}
