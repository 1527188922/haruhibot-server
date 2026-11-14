package com.haruhi.botserver.features.ai.support.tool;

import com.alibaba.fastjson.JSONObject;
import com.haruhi.botserver.bot.session.Bot;
import com.haruhi.botserver.features.ai.model.AiSessionState;

/**
 * 伪 Agent 工具白名单条目。
 */
public interface AiTool {

    String name();

    String execute(Bot bot, AiSessionState state, JSONObject args) throws Exception;
}
