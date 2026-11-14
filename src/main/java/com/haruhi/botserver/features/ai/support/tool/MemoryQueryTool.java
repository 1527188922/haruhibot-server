package com.haruhi.botserver.features.ai.support.tool;

import com.alibaba.fastjson.JSONObject;
import com.haruhi.botserver.bot.session.Bot;
import com.haruhi.botserver.features.ai.model.AiSessionState;
import com.haruhi.botserver.features.ai.service.AiMemoryService;
import org.springframework.stereotype.Component;

/**
 * 查询当前会话的长期记忆。
 */
@Component
public class MemoryQueryTool implements AiTool {

    private final AiMemoryService memoryService;

    public MemoryQueryTool(AiMemoryService memoryService) {
        this.memoryService = memoryService;
    }

    @Override
    public String name() {
        return "memory_query";
    }

    @Override
    public String execute(Bot bot, AiSessionState state, JSONObject args) {
        if (!memoryService.enabled()) {
            return "长期记忆未启用。";
        }
        int limit = args == null ? 10 : args.getIntValue("limit");
        if (limit <= 0) {
            limit = 10;
        }
        String formatted = memoryService.format(memoryService.list(state, limit));
        return formatted.isBlank() ? "当前会话没有长期记忆。" : formatted;
    }
}
