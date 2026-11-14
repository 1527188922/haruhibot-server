package com.haruhi.botserver.features.ai.service;

import com.alibaba.fastjson.JSONObject;
import com.haruhi.botserver.bot.session.Bot;
import com.haruhi.botserver.features.ai.model.AiSessionState;
import com.haruhi.botserver.features.ai.model.AiToolCall;
import com.haruhi.botserver.features.ai.support.AiChatSettings;
import com.haruhi.botserver.features.ai.support.tool.AiTool;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
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
                .filter(tools::containsKey)
                .toList();
    }

    public String execute(Bot bot, AiSessionState state, AiToolCall call) {
        if (call == null || StringUtils.isBlank(call.getName())) {
            return "工具调用失败：缺少工具名。";
        }
        String name = call.getName();
        if (!AiChatSettings.agentEnabled() || !AiChatSettings.agentTools().contains(name)) {
            return "工具不可用：" + name;
        }
        AiTool tool = tools.get(name);
        if (tool == null) {
            return "工具未注册：" + name;
        }
        String sessionKey = state == null ? "anonymous" : state.getKey().cacheKey();
        if (!allow(sessionKey)) {
            return "工具调用太频繁，请稍后再试。";
        }
        try {
            String result = tool.execute(bot, state, call.getArgs() == null ? new JSONObject() : call.getArgs());
            if (StringUtils.isBlank(result)) {
                result = "工具没有返回内容。";
            }
            int max = AiChatSettings.toolMaxResultChars();
            if (result.length() > max) {
                result = result.substring(0, max) + "…";
            }
            return result;
        } catch (Exception e) {
            log.warn("AI工具执行失败: {} session={}", name, sessionKey, e);
            return "工具执行失败：" + name + "（" + e.getMessage() + "）";
        }
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
