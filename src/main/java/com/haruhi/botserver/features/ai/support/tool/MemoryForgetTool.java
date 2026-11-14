package com.haruhi.botserver.features.ai.support.tool;

import com.alibaba.fastjson.JSONObject;
import com.haruhi.botserver.bot.session.Bot;
import com.haruhi.botserver.features.ai.model.AiSessionState;
import com.haruhi.botserver.features.ai.service.AiMemoryService;
import org.springframework.stereotype.Component;

/**
 * 忘记一条长期记忆。
 */
@Component
public class MemoryForgetTool implements AiTool {

    private final AiMemoryService memoryService;

    public MemoryForgetTool(AiMemoryService memoryService) {
        this.memoryService = memoryService;
    }

    @Override
    public String name() {
        return "memory_forget";
    }

    @Override
    public String execute(Bot bot, AiSessionState state, JSONObject args) {
        String key = args == null ? null : args.getString("key");
        if (key == null || key.isBlank()) {
            return "参数错误：需要 key。";
        }
        if (!memoryService.enabled()) {
            return "长期记忆未启用。";
        }
        return memoryService.forget(state, key) ? "已忘记：" + key : "没有找到这条记忆：" + key;
    }
}
