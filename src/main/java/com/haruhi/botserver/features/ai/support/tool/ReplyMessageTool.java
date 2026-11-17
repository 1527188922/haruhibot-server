package com.haruhi.botserver.features.ai.support.tool;

import com.alibaba.fastjson.JSONObject;
import com.haruhi.botserver.bot.session.Bot;
import com.haruhi.botserver.features.ai.model.AiSessionState;
import com.haruhi.botserver.features.ai.support.AiMessageExtractor;
import com.haruhi.botserver.integration.onebot.model.Message;
import com.haruhi.botserver.integration.onebot.model.SyncResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

/**
 * 读取当前消息引用/回复的那条消息内容。
 */
@Component
@Slf4j
public class ReplyMessageTool implements AiTool {

    private static final long GET_REPLY_TIMEOUT = 2500L;
    private static final int MAX_CONTENT_CHARS = 1200;
    private static final Pattern CQ = Pattern.compile("\\[CQ:[^\\]]*]");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public String name() {
        return "reply_message";
    }

    @Override
    public String execute(Bot bot, AiSessionState state, JSONObject args) {
        if (bot == null || state == null) {
            return "当前没有可用的机器人连接。";
        }
        String replyId = state.getCurrentReplyMessageId();
        if (StringUtils.isBlank(replyId)) {
            return "当前消息没有引用其他消息。";
        }
        if (state.getKey().group() && !state.isCurrentDirect()) {
            return "当前群消息没有 @ 或提及机器人，不能读取引用消息。";
        }
        try {
            SyncResponse<Message> response = bot.getMsg(replyId, GET_REPLY_TIMEOUT);
            Message reply = response == null ? null : response.getData();
            if (response == null || !response.isSuccess() || reply == null) {
                return "引用消息获取失败：可能已被撤回或超出可查询范围。";
            }

            String content = CQ.matcher(AiMessageExtractor.buildText(reply)).replaceAll("").trim();
            if (content.isBlank()) {
                content = "[空消息]";
            }
            if (content.length() > MAX_CONTENT_CHARS) {
                content = content.substring(0, MAX_CONTENT_CHARS) + "…";
            }

            StringBuilder sb = new StringBuilder("引用消息：\n");
            sb.append("发送者：");
            if (reply.getUserId() == null) {
                sb.append("未知");
            } else {
                String name = AiMessageExtractor.displayName(reply.getSender(), reply.getUserId());
                sb.append(StringUtils.defaultIfBlank(name, "未知"))
                        .append("(").append(reply.getUserId()).append(")");
            }
            sb.append('\n');
            if (reply.getTime() != null && reply.getTime() > 0) {
                sb.append("时间：").append(formatTime(reply.getTime())).append('\n');
            }
            sb.append("内容：").append(content);
            return sb.toString();
        } catch (Exception e) {
            log.warn("获取引用消息失败: id={}", replyId, e);
            return "引用消息获取失败：" + StringUtils.defaultIfBlank(e.getMessage(), e.getClass().getSimpleName());
        }
    }

    private String formatTime(long seconds) {
        return TIME_FORMAT.format(LocalDateTime.ofInstant(
                Instant.ofEpochSecond(seconds), ZoneId.systemDefault()));
    }
}
