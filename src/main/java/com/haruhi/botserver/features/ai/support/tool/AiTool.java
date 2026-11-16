package com.haruhi.botserver.features.ai.support.tool;

import com.alibaba.fastjson.JSONObject;
import com.haruhi.botserver.bot.session.Bot;
import com.haruhi.botserver.features.ai.model.AiSessionState;

/**
 * 伪 Agent 工具白名单条目。
 */
public interface AiTool {

    String name();

    /**
     * 是否具备执行条件（例如联网搜索需要 API Key）；不可用的工具不会注入提示词。
     */
    default boolean available() {
        return true;
    }

    String execute(Bot bot, AiSessionState state, JSONObject args) throws Exception;
}
