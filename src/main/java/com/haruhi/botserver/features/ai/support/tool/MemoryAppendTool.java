package com.haruhi.botserver.features.ai.support.tool;

import com.alibaba.fastjson.JSONObject;
import com.haruhi.botserver.bot.session.Bot;
import com.haruhi.botserver.features.ai.model.AiSessionState;
import com.haruhi.botserver.features.ai.service.AiMemoryService;
import org.springframework.stereotype.Component;

/**
 * 记住一条会话级长期记忆。
 */
@Component
public class MemoryAppendTool implements AiTool {

    private final AiMemoryService memoryService;

    public MemoryAppendTool(AiMemoryService memoryService) {
        this.memoryService = memoryService;
    }

    @Override
    public String name() {
        return "memory_append";
    }

    @Override
    public String execute(Bot bot, AiSessionState state, JSONObject args) {
        String key = args == null ? null : args.getString("key");
        String value = args == null ? null : args.getString("value");
        if (key == null || key.isBlank() || value == null || value.isBlank()) {
            return "参数错误：需要 key 和 value。";
        }
        if (!memoryService.enabled()) {
            return "长期记忆未启用。";
        }
        boolean ok = memoryService.remember(state, key, value);
        return ok ? "已记住：" + key : "记忆保存失败（可能内容为空或超长）。";
    }
}
