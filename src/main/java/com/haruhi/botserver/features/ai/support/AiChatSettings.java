package com.haruhi.botserver.features.ai.support;

import com.haruhi.botserver.configuration.metadata.ConfigKey;
import com.haruhi.botserver.configuration.service.Configs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * ds.chat.* 配置读取。所有方法都直接读 Configs 快照，天然支持热更新。
 */
public final class AiChatSettings {

    private AiChatSettings() {
    }

    public static boolean enabled() {
        return Configs.getBool(ConfigKey.DS_CHAT_ENABLE, false);
    }

    public static String model() {
        String model = Configs.getStr(ConfigKey.DS_CHAT_MODEL, "deepseek-flash");
        return "deepseek-v4-pro".equalsIgnoreCase(model) ? "deepseek-v4-pro" : "deepseek-flash";
    }

    public static boolean thinkingEnabled() {
        return "enabled".equalsIgnoreCase(Configs.getStr(ConfigKey.DS_CHAT_THINKING, "disabled"));
    }

    public static String reasoningEffort() {
        return Configs.getStr(ConfigKey.DS_CHAT_REASONING_EFFORT, "high");
    }

    public static double temperature() {
        return parseDouble(Configs.getStr(ConfigKey.DS_CHAT_TEMPERATURE, "1.1"), 1.1);
    }

    public static int maxTokens() {
        return Math.max(128, Configs.getInt(ConfigKey.DS_CHAT_MAX_TOKENS, 1024));
    }

    public static int retry() {
        return Math.max(0, Configs.getInt(ConfigKey.DS_CHAT_RETRY, 2));
    }

    public static int maxConcurrent() {
        return Math.max(1, Configs.getInt(ConfigKey.DS_CHAT_MAX_CONCURRENT, 4));
    }

    public static int dailyTokenLimit() {
        return Math.max(0, Configs.getInt(ConfigKey.DS_CHAT_DAILY_TOKEN_LIMIT, 0));
    }

    public static int sessionExpireMinutes() {
        return Math.max(1, Configs.getInt(ConfigKey.DS_CHAT_SESSION_EXPIRE_MINUTES, 120));
    }

    public static int historyMaxTurns() {
        return Math.max(1, Configs.getInt(ConfigKey.DS_CHAT_HISTORY_MAX_TURNS, 20));
    }

    public static int historyMaxInputTokens() {
        return Math.max(512, Configs.getInt(ConfigKey.DS_CHAT_HISTORY_MAX_INPUT_TOKENS, 12000));
    }

    public static String defaultRole() {
        return Configs.getStr(ConfigKey.DS_CHAT_ROLE, "春日酱");
    }

    public static int roleMaxChars() {
        return Math.max(200, Configs.getInt(ConfigKey.DS_CHAT_ROLE_MAX_CHARS, 6000));
    }

    public static long dedupeTtlMillis() {
        return Math.max(1, Configs.getInt(ConfigKey.DS_CHAT_DEDUPE_TTL_SECONDS, 300)) * 1000L;
    }

    public static List<Long> ignoreUserIds() {
        return Configs.getList(ConfigKey.DS_CHAT_IGNORE_USER_IDS, Long.class, Collections.emptyList());
    }

    public static boolean visionEnabled() {
        return Configs.getBool(ConfigKey.DS_CHAT_VISION_ENABLE, false)
                && "deepseek-flash".equals(model());
    }

    // ==================== 群聊 ====================

    public static List<Long> groupWhitelist() {
        return Configs.getList(ConfigKey.DS_CHAT_GROUP_WHITELIST, Long.class, Collections.emptyList());
    }

    public static boolean groupEnabled(long groupId) {
        return enabled() && groupWhitelist().contains(groupId);
    }

    public static boolean privateEnabled() {
        return enabled() && Configs.getBool(ConfigKey.DS_CHAT_PRIVATE_ENABLE, false);
    }

    public static boolean triggerAt() {
        return Configs.getBool(ConfigKey.DS_CHAT_GROUP_TRIGGER_AT, true);
    }

    public static boolean triggerReply() {
        return Configs.getBool(ConfigKey.DS_CHAT_GROUP_TRIGGER_REPLY, true);
    }

    public static boolean triggerRole() {
        return Configs.getBool(ConfigKey.DS_CHAT_GROUP_TRIGGER_ROLE, true);
    }

    public static boolean contextJudge() {
        return Configs.getBool(ConfigKey.DS_CHAT_GROUP_TRIGGER_CONTEXT_JUDGE, true);
    }

    public static List<String> addressWords() {
        return Configs.getList(ConfigKey.DS_CHAT_GROUP_TRIGGER_ADDRESS_WORDS, String.class,
                List.of("你", "您", "在吗", "出来", "帮我", "你觉得", "你说", "看看", "怎么办", "咋办"));
    }

    public static String triggerPrefix() {
        String value = Configs.getStr(ConfigKey.DS_CHAT_GROUP_TRIGGER_PREFIX, "");
        return value == null ? "" : value.trim();
    }

    public static List<String> triggerKeywords() {
        return Configs.getList(ConfigKey.DS_CHAT_GROUP_TRIGGER_KEYWORDS, String.class, Collections.emptyList());
    }

    public static int randomPercent() {
        return Math.max(0, Math.min(100, Configs.getInt(ConfigKey.DS_CHAT_GROUP_TRIGGER_RANDOM_PERCENT, 0)));
    }

    public static int ambientSize() {
        return Math.max(0, Configs.getInt(ConfigKey.DS_CHAT_GROUP_AMBIENT_SIZE, 20));
    }

    public static long groupMinReplyIntervalMillis() {
        return Math.max(0, Configs.getInt(ConfigKey.DS_CHAT_GROUP_MIN_REPLY_INTERVAL_MS, 3000));
    }

    public static int groupMaxRepliesPerMinute() {
        return Math.max(1, Configs.getInt(ConfigKey.DS_CHAT_GROUP_MAX_REPLIES_PER_MINUTE, 6));
    }

    public static long groupUserCooldownMillis() {
        return Math.max(0, Configs.getInt(ConfigKey.DS_CHAT_GROUP_USER_COOLDOWN_MS, 10000));
    }

    public static boolean replyAtOnly() {
        return Configs.getBool(ConfigKey.DS_CHAT_GROUP_REPLY_AT_ONLY, true);
    }

    public static long mentionWaitMillis(String text) {
        int min = Math.max(0, Configs.getInt(ConfigKey.DS_CHAT_GROUP_MENTION_DELAY_MIN_MS, 5000));
        int max = Math.max(min, Configs.getInt(ConfigKey.DS_CHAT_GROUP_MENTION_DELAY_MAX_MS, 15000));
        // 文本越短越靠上限，完整问题取下限附近
        int length = text == null ? 0 : text.length();
        double ratio = length >= 30 ? 0.0 : Math.max(0.0, (30 - length) / 30.0);
        int base = (int) Math.round(min + (max - min) * ratio);
        int jitter = Math.max(1, (max - min) / 5);
        return Math.max(min, Math.min(max, base + ThreadLocalRandom.current().nextInt(jitter + 1)));
    }

    public static long atOnlyWaitMillis() {
        int min = Math.max(0, Configs.getInt(ConfigKey.DS_CHAT_GROUP_AT_ONLY_DELAY_MIN_MS, 15000));
        int max = Math.max(min, Configs.getInt(ConfigKey.DS_CHAT_GROUP_AT_ONLY_DELAY_MAX_MS, 25000));
        return ThreadLocalRandom.current().nextLong(min, (long) max + 1);
    }

    public static long mentionCapMillis() {
        return Math.max(1000, Configs.getInt(ConfigKey.DS_CHAT_GROUP_MENTION_DELAY_CAP_MS, 30000));
    }

    // ==================== 私聊/合并 ====================

    public static long privateMinReplyIntervalMillis() {
        return Math.max(0, Configs.getInt(ConfigKey.DS_CHAT_PRIVATE_MIN_REPLY_INTERVAL_MS, 1000));
    }

    public static long debounceWaitMillis() {
        int min = Math.max(0, Configs.getInt(ConfigKey.DS_CHAT_DEBOUNCE_MIN_MS, 2000));
        int max = Math.max(min, Configs.getInt(ConfigKey.DS_CHAT_DEBOUNCE_MAX_MS, 6000));
        if (max == 0) {
            return 0;
        }
        return ThreadLocalRandom.current().nextLong(min, (long) max + 1);
    }

    public static long debounceMaxWaitMillis() {
        return Math.max(1000, Configs.getInt(ConfigKey.DS_CHAT_DEBOUNCE_MAX_WAIT_MS, 15000));
    }

    // ==================== 发送 ====================

    public static int messageMaxCount() {
        return Math.max(1, Configs.getInt(ConfigKey.DS_CHAT_MESSAGE_MAX_COUNT, 4));
    }

    public static int messageMaxChars() {
        return Math.max(20, Configs.getInt(ConfigKey.DS_CHAT_MESSAGE_MAX_CHARS, 300));
    }

    public static int messageMaxDelayMillis() {
        return Math.max(0, Configs.getInt(ConfigKey.DS_CHAT_MESSAGE_MAX_DELAY_MS, 5000));
    }

    public static int messageFirstDelayMillis() {
        return Math.max(0, Configs.getInt(ConfigKey.DS_CHAT_MESSAGE_FIRST_DELAY_MS, 300));
    }

    public static int messageTotalTimeoutMillis() {
        return Math.max(1000, Configs.getInt(ConfigKey.DS_CHAT_MESSAGE_TOTAL_TIMEOUT_MS, 30000));
    }

    // ==================== 伪 Agent ====================

    public static boolean agentEnabled() {
        return Configs.getBool(ConfigKey.DS_CHAT_AGENT_ENABLE, true);
    }

    public static int agentMaxSteps() {
        return Math.max(0, Configs.getInt(ConfigKey.DS_CHAT_AGENT_MAX_STEPS, 2));
    }

    public static List<String> agentTools() {
        return Configs.getList(ConfigKey.DS_CHAT_AGENT_TOOLS, String.class,
                List.of("group_history", "member_info"));
    }

    public static long toolTimeoutMillis() {
        return Math.max(1000, Configs.getInt(ConfigKey.DS_CHAT_AGENT_TOOL_TIMEOUT_MS, 5000));
    }

    public static int toolMaxResultChars() {
        return Math.max(200, Configs.getInt(ConfigKey.DS_CHAT_AGENT_MAX_RESULT_CHARS, 2000));
    }

    public static int toolCallsPerTurn() {
        return Math.max(0, Configs.getInt(ConfigKey.DS_CHAT_AGENT_CALLS_PER_TURN, 2));
    }

    public static int toolCallsPerMinute() {
        return Math.max(1, Configs.getInt(ConfigKey.DS_CHAT_AGENT_CALLS_PER_MINUTE, 6));
    }

    // ==================== 持久化/余额 ====================

    public static boolean persistenceEnabled() {
        return Configs.getBool(ConfigKey.DS_CHAT_PERSISTENCE_ENABLE, true);
    }

    public static int maxMessagesPerSession() {
        return Math.max(20, Configs.getInt(ConfigKey.DS_CHAT_PERSISTENCE_MAX_MESSAGES_PER_SESSION, 200));
    }

    public static int balanceRefreshMinutes() {
        return Math.max(0, Configs.getInt(ConfigKey.DS_CHAT_BALANCE_REFRESH_MINUTES, 10));
    }

    // ==================== 角色映射 ====================

    public static String groupRole(long groupId) {
        return mapGet(ConfigKey.DS_CHAT_GROUP_ROLES, String.valueOf(groupId));
    }

    public static String privateRole(long userId) {
        return mapGet(ConfigKey.DS_CHAT_PRIVATE_ROLES, String.valueOf(userId));
    }

    private static String mapGet(ConfigKey key, String id) {
        for (String item : Configs.getStrList(key)) {
            int index = item.indexOf('=');
            if (index <= 0) {
                continue;
            }
            if (item.substring(0, index).trim().equals(id)) {
                return item.substring(index + 1).trim();
            }
        }
        return null;
    }

    private static double parseDouble(String value, double fallback) {
        try {
            return Double.parseDouble(value);
        } catch (Exception e) {
            return fallback;
        }
    }

    /**
     * 用于 WebUI/日志展示的当前配置快照。
     */
    public static Map<String, Object> snapshot() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("enabled", enabled());
        map.put("model", model());
        map.put("thinking", thinkingEnabled());
        map.put("groupWhitelist", new ArrayList<>(groupWhitelist()));
        map.put("privateEnabled", privateEnabled());
        map.put("randomPercent", randomPercent());
        map.put("defaultRole", defaultRole());
        map.put("agentEnabled", agentEnabled());
        map.put("tools", new ArrayList<>(agentTools()));
        return map;
    }

    public static String lower(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT);
    }
}
