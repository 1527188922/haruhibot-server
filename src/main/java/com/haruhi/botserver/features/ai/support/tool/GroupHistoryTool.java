package com.haruhi.botserver.features.ai.support.tool;

import com.alibaba.fastjson.JSONObject;
import com.haruhi.botserver.bot.session.Bot;
import com.haruhi.botserver.features.ai.model.AiSessionState;
import com.haruhi.botserver.features.chatrecord.persistence.entity.ChatRecordGroup;
import com.haruhi.botserver.features.chatrecord.service.ChatRecordService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 查当前群最近聊天记录。
 */
@Component
public class GroupHistoryTool implements AiTool {

    private static final Pattern CQ = Pattern.compile("\\[CQ:[^\\]]*]");

    private final ChatRecordService chatRecordService;

    public GroupHistoryTool(ChatRecordService chatRecordService) {
        this.chatRecordService = chatRecordService;
    }

    @Override
    public String name() {
        return "group_history";
    }

    @Override
    public String execute(Bot bot, AiSessionState state, JSONObject args) {
        if (state == null || !state.getKey().group()) {
            return "当前不是群聊，不能查询群历史。";
        }
        int limit = args == null ? 10 : args.getIntValue("limit");
        if (limit <= 0) {
            limit = 10;
        }
        limit = Math.min(limit, 30);
        List<ChatRecordGroup> messages = chatRecordService.recentGroupMessages(state.getKey().targetId(), limit);
        if (messages.isEmpty()) {
            return "当前群没有可查询的聊天记录。";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("最近 ").append(messages.size()).append(" 条群聊（按时间正序）：\n");
        for (ChatRecordGroup message : messages) {
            String name = StringUtils.defaultIfBlank(message.getCard(), message.getNickname());
            if (StringUtils.isBlank(name)) {
                name = String.valueOf(message.getUserId());
            }
            String content = CQ.matcher(StringUtils.defaultString(message.getContent())).replaceAll("");
            content = content.replaceAll("\\s+", " ").trim();
            sb.append("[").append(StringUtils.defaultString(message.getTime())).append("] ")
                    .append(name).append(": ").append(limit(content, 120)).append('\n');
        }
        return sb.toString();
    }

    private String limit(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max) + "…";
    }
}
