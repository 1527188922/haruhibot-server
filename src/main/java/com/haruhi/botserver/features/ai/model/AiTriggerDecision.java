package com.haruhi.botserver.features.ai.model;

/**
 * 触发判定结果。
 */
public record AiTriggerDecision(
        boolean triggered,
        boolean direct,
        boolean atOnly,
        boolean random,
        long waitMillis,
        String reason
) {

    public static AiTriggerDecision no(String reason) {
        return new AiTriggerDecision(false, false, false, false, 0, reason);
    }

    public static AiTriggerDecision direct(long waitMillis, String reason) {
        return new AiTriggerDecision(true, true, false, false, waitMillis, reason);
    }

    public static AiTriggerDecision atOnly(long waitMillis) {
        return new AiTriggerDecision(true, true, true, false, waitMillis, "at-only");
    }

    public static AiTriggerDecision random(long waitMillis) {
        return new AiTriggerDecision(true, false, false, true, waitMillis, "random");
    }
}
