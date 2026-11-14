package com.haruhi.botserver.features.ai.handler;

import com.haruhi.botserver.bot.handler.HandlerWeightEnum;
import com.haruhi.botserver.bot.handler.IAllMessageHandler;
import com.haruhi.botserver.bot.session.Bot;
import com.haruhi.botserver.features.ai.client.deepseek.DeepSeekBalanceResponse;
import com.haruhi.botserver.features.ai.client.deepseek.DeepSeekClient;
import com.haruhi.botserver.features.ai.model.AiSessionKey;
import com.haruhi.botserver.features.ai.model.AiSessionState;
import com.haruhi.botserver.features.ai.service.AiSessionService;
import com.haruhi.botserver.features.ai.support.prompt.RoleCardService;
import com.haruhi.botserver.integration.onebot.model.Message;
import com.haruhi.botserver.integration.onebot.model.MessageHolder;
import com.haruhi.botserver.shared.annotation.SuperuserAuthentication;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * ds 控制命令（W_870）。所有命令仅超级管理员可用；未知命令返回 false。
 */
@Component
@Slf4j
public class DeepSeekCommandHandler implements IAllMessageHandler {

    private final AiSessionService sessionService;
    private final RoleCardService roleCardService;
    private final DeepSeekClient deepSeekClient;

    public DeepSeekCommandHandler(AiSessionService sessionService,
                                  RoleCardService roleCardService,
                                  DeepSeekClient deepSeekClient) {
        this.sessionService = sessionService;
        this.roleCardService = roleCardService;
        this.deepSeekClient = deepSeekClient;
    }

    @Override
    public int weight() {
        return HandlerWeightEnum.W_870.getWeight();
    }

    @Override
    public String funName() {
        return HandlerWeightEnum.W_870.getName();
    }

    /**
     * 权限由 {@link SuperuserAuthentication} 统一校验：非超级管理员在 AOP 层直接返回 false，
     * 不会进入本方法；这里只负责命令解析。
     */
    @Override
    @SuperuserAuthentication
    public boolean onMessage(Bot bot, Message message) {
        if (!message.isTextMsg() || message.isSelfMsg()) {
            return false;
        }
        if (message.isGroupMsg() && !message.isAtBot()) {
            return false;
        }
        String text = StringUtils.trimToEmpty(message.getText(-1));
        if (!text.equals("ds") && !text.startsWith("ds ")) {
            return false;
        }
        String command = text.length() <= 2 ? "" : text.substring(3).trim();
        if (command.isEmpty() || "help".equalsIgnoreCase(command)) {
            reply(bot, message, helpText());
            return true;
        }
        AiSessionKey key = AiSessionKey.of(message);
        AiSessionState state = sessionService.load(key);
        if ("reset".equalsIgnoreCase(command)) {
            sessionService.reset(state);
            reply(bot, message, "已重置当前会话。");
            return true;
        }
        if ("status".equalsIgnoreCase(command)) {
            reply(bot, message, statusText(key, state));
            return true;
        }
        if ("on".equalsIgnoreCase(command)) {
            sessionService.updateStatus(state, "active");
            reply(bot, message, "已恢复当前会话。");
            return true;
        }
        if ("off".equalsIgnoreCase(command)) {
            sessionService.updateStatus(state, "paused");
            reply(bot, message, "已暂停当前会话；新消息不会排队。");
            return true;
        }
        if (command.equals("role") || command.startsWith("role ")) {
            String role = command.length() <= 4 ? "" : command.substring(4).trim();
            if (role.isEmpty() || "list".equalsIgnoreCase(role)) {
                reply(bot, message, "可用角色：\n" + String.join("\n", roleCardService.listRoleNames()));
                return true;
            }
            RoleCardService.RoleCard card = roleCardService.load(role);
            if (card == null) {
                reply(bot, message, "角色不存在：" + role + "\n可用角色：\n"
                        + String.join("\n", roleCardService.listRoleNames()));
                return true;
            }
            sessionService.updateRole(state, card.name());
            reply(bot, message, "已切换角色：" + card.name());
            return true;
        }
        return false;
    }

    private String helpText() {
        return """
                ds help       查看命令
                ds reset      清空当前会话历史
                ds status     查看会话/角色/token/余额
                ds role list  列出角色卡
                ds role 名称  切换当前会话角色
                ds on / off   恢复 / 暂停当前会话
                """.trim();
    }

    private String statusText(AiSessionKey key, AiSessionState state) {
        StringBuilder sb = new StringBuilder();
        sb.append("会话：").append(key.group() ? "群 " + key.targetId() : "私聊 " + key.targetId()).append('\n');
        sb.append("角色：").append(StringUtils.defaultIfBlank(state.roleName(), "全局默认")).append('\n');
        sb.append("状态：").append(state.paused() ? "paused" : "active").append('\n');
        sb.append("历史：").append(state.getHistory().size()).append(" 条\n");
        sb.append("今日 token：").append(sessionService.todayTokens(state)).append('\n');
        String balance;
        try {
            DeepSeekBalanceResponse response = deepSeekClient.getBalance();
            List<String> lines = DeepSeekClient.formatBalance(response);
            balance = lines.isEmpty() ? "无余额信息" : String.join("\n", lines);
            if (!response.isAvailable()) {
                balance = balance + "（不可用）";
            }
        } catch (Exception e) {
            balance = "查询失败";
        }
        sb.append("余额：").append(balance);
        return sb.toString();
    }

    private void reply(Bot bot, Message message, String text) {
        if (message.isGroupMsg()) {
            bot.sendGroupMessage(message.getGroupId(), MessageHolder.instanceText(text));
        } else {
            bot.sendPrivateMessage(message.getUserId(), MessageHolder.instanceText(text));
        }
    }
}
