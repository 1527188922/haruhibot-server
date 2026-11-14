package com.haruhi.botserver.features.ai.support.tool;

import com.alibaba.fastjson.JSONObject;
import com.haruhi.botserver.bot.session.Bot;
import com.haruhi.botserver.features.ai.model.AiSessionState;
import com.haruhi.botserver.features.ai.support.AiChatSettings;
import com.haruhi.botserver.integration.onebot.model.GroupMember;
import com.haruhi.botserver.integration.onebot.model.SyncResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * 查当前群成员名片/昵称。
 */
@Component
public class MemberInfoTool implements AiTool {

    @Override
    public String name() {
        return "member_info";
    }

    @Override
    public String execute(Bot bot, AiSessionState state, JSONObject args) {
        if (state == null || !state.getKey().group()) {
            return "当前不是群聊，不能查询群成员。";
        }
        String qq = args == null ? null : args.getString("qq");
        if (StringUtils.isBlank(qq) || !qq.matches("\\d{5,15}")) {
            return "参数错误：qq 必须是数字。";
        }
        if (bot == null || !bot.isOnline()) {
            return "机器人当前不在线，查询失败。";
        }
        try {
            SyncResponse<GroupMember> response = bot.getGroupMemberInfo(
                    state.getKey().targetId(), Long.parseLong(qq), AiChatSettings.toolTimeoutMillis());
            if (response == null || !response.isSuccess() || response.getData() == null) {
                return "查询失败：当前群没有找到该成员。";
            }
            GroupMember member = response.getData();
            String name = StringUtils.defaultIfBlank(member.getCard(), member.getNickname());
            return "QQ: " + qq + "\n昵称: " + StringUtils.defaultIfBlank(member.getNickname(), "未知")
                    + "\n群名片: " + StringUtils.defaultIfBlank(member.getCard(), "无")
                    + "\n角色: " + StringUtils.defaultIfBlank(member.getRole(), "member")
                    + "\n显示名: " + StringUtils.defaultIfBlank(name, qq);
        } catch (Exception e) {
            return "查询失败：" + e.getMessage();
        }
    }
}
