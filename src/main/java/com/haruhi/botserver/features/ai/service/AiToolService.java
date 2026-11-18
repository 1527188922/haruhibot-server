package com.haruhi.botserver.features.ai.service;

import com.alibaba.fastjson.JSONObject;
import com.haruhi.botserver.bot.session.Bot;
import com.haruhi.botserver.features.ai.model.AiSessionState;
import com.haruhi.botserver.features.ai.model.AiToolCall;
import com.haruhi.botserver.features.ai.support.AiChatSettings;
import com.haruhi.botserver.features.ai.support.tool.AiTool;
import com.haruhi.botserver.infrastructure.logging.DbLog;
import com.haruhi.botserver.shared.constant.BusinessModuleEnum;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 工具白名单、作用域、频率与结果截断。
 */
@Service
public class AiToolService {

    private final Map<String, AiTool> tools = new LinkedHashMap<>();
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    public AiToolService(List<AiTool> toolList) {
        for (AiTool tool : toolList) {
            tools.put(tool.name(), tool);
        }
    }

    public List<String> enabledTools() {
        return AiChatSettings.agentTools().stream()
                .filter(name -> {
                    AiTool tool = tools.get(name);
                    return tool != null && tool.available();
                })
                .toList();
    }

    public String execute(Bot bot, AiSessionState state, AiToolCall call) {
        String sessionKey = state == null || state.getKey() == null ? "anonymous" : state.getKey().cacheKey();
        if (call == null || StringUtils.isBlank(call.getName())) {
            DbLog.warn(BusinessModuleEnum.AI_CHAT, "AI工具调用失败：缺少工具名 session:{}", sessionKey);
            return "工具调用失败：缺少工具名。";
        }
        String name = call.getName();
        if (!AiChatSettings.agentEnabled() || !AiChatSettings.agentTools().contains(name)) {
            DbLog.warn(BusinessModuleEnum.AI_CHAT, "AI工具未启用，调用被拒绝 tool:{} session:{} args:{}",
                    name, sessionKey, snippet(call.getArgs()));
            return "工具不可用：" + name;
        }
        AiTool tool = tools.get(name);
        if (tool == null) {
            DbLog.warn(BusinessModuleEnum.AI_CHAT, "AI工具未注册 tool:{} session:{}", name, sessionKey);
            return "工具未注册：" + name;
        }
        if (!tool.available()) {
            DbLog.warn(BusinessModuleEnum.AI_CHAT, "AI工具暂不可用 tool:{} session:{}", name, sessionKey);
            return "工具暂不可用：" + name + "（未开启或缺少 API Key）";
        }
        if (!allow(sessionKey)) {
            DbLog.warn(BusinessModuleEnum.AI_CHAT, "AI工具调用过于频繁 tool:{} session:{}", name, sessionKey);
            return "工具调用太频繁，请稍后再试。";
        }
        long start = System.currentTimeMillis();
        try {
            String result = tool.execute(bot, state, call.getArgs() == null ? new JSONObject() : call.getArgs());
            if (StringUtils.isBlank(result)) {
                result = "工具没有返回内容。";
            }
            int max = AiChatSettings.toolMaxResultChars();
            if (result.length() > max) {
                result = result.substring(0, max) + "…";
            }
            DbLog.info(BusinessModuleEnum.AI_CHAT, "AI工具调用完成 tool:{} session:{} elapsed:{}ms args:{} result:{}",
                    name, sessionKey, System.currentTimeMillis() - start, snippet(call.getArgs()), snippet(result));
            return result;
        } catch (Exception e) {
            DbLog.error(BusinessModuleEnum.AI_CHAT, "AI工具执行失败 tool:{} session:{} elapsed:{}ms",
                    name, sessionKey, System.currentTimeMillis() - start, e);
            return "工具执行失败：" + name + "（" + e.getMessage() + "）";
        }
    }

    private String snippet(Object value) {
        if (value == null) {
            return "";
        }
        String text = String.valueOf(value).replaceAll("\\s+", " ").trim();
        return text.length() <= 200 ? text : text.substring(0, 200) + "…";
    }

    private boolean allow(String sessionKey) {
        long now = System.currentTimeMillis();
        Window window = windows.computeIfAbsent(sessionKey, ignored -> new Window());
        synchronized (window) {
            if (window.minute != now / 60000) {
                window.minute = now / 60000;
                window.count = 0;
            }
            if (window.count >= AiChatSettings.toolCallsPerMinute()) {
                return false;
            }
            window.count++;
            return true;
        }
    }

    private static class Window {
        private long minute;
        private int count;
    }
}
