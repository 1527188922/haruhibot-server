package com.haruhi.botserver.features.ai.service;

import com.haruhi.botserver.bot.session.Bot;
import com.haruhi.botserver.features.ai.model.AiIncoming;
import com.haruhi.botserver.features.ai.model.AiSessionKey;
import com.haruhi.botserver.features.ai.model.AiTriggerDecision;
import com.haruhi.botserver.features.ai.support.AiChatSettings;
import com.haruhi.botserver.features.ai.support.prompt.RoleCardService;
import com.haruhi.botserver.integration.onebot.model.Message;
import com.haruhi.botserver.integration.onebot.model.SyncResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

/**
 * 触发判定：@、引用、角色触发词、前缀、补充词、随机接话。
 */
@Service
@Slf4j
public class AiTriggerPolicy {

    private static final Pattern URL = Pattern.compile("https?://\\S+", Pattern.CASE_INSENSITIVE);
    private static final Pattern CODE = Pattern.compile("```[\\s\\S]*?```");
    private static final Pattern THIRD_PARTY = Pattern.compile("他说|她说|所谓|转述|引用|听说|别人说");

    private final RoleCardService roleCardService;
    private final AiSessionService sessionService;

    public AiTriggerPolicy(RoleCardService roleCardService, AiSessionService sessionService) {
        this.roleCardService = roleCardService;
        this.sessionService = sessionService;
    }

    public AiTriggerDecision decide(Bot bot, Message message, AiIncoming incoming) {
        if (incoming == null) {
            return AiTriggerDecision.no("empty");
        }
        if (incoming.getUserId() > 0 && AiChatSettings.ignoreUserIds().contains(incoming.getUserId())) {
            return AiTriggerDecision.no("ignored-user");
        }
        String text = StringUtils.defaultString(incoming.getText());
        if (message.isPrivateMsg()) {
            if (text.isBlank() && !incoming.isHasImage()) {
                return AiTriggerDecision.no("empty-private");
            }
            return new AiTriggerDecision(true, false, false, false,
                    AiChatSettings.debounceWaitMillis(), "private");
        }
        if (!message.isGroupMsg() || message.getGroupId() == null) {
            return AiTriggerDecision.no("not-group");
        }

        if (incoming.isAtOnly()) {
            return AiTriggerDecision.atOnly(AiChatSettings.atOnlyWaitMillis());
        }

        // 直接触发
        if (AiChatSettings.triggerAt() && message.isAtBot()) {
            return AiTriggerDecision.direct(AiChatSettings.mentionWaitMillis(text), "at");
        }
        if (AiChatSettings.triggerReply() && isReplyToBot(bot, incoming)) {
            return AiTriggerDecision.direct(AiChatSettings.mentionWaitMillis(text), "reply");
        }

        String prefix = AiChatSettings.triggerPrefix();
        if (StringUtils.isNotBlank(prefix) && text.startsWith(prefix)) {
            return AiTriggerDecision.direct(AiChatSettings.mentionWaitMillis(text), "prefix");
        }

        if (AiChatSettings.triggerRole()) {
            RoleCardService.RoleCard role = roleCardService.resolve(
                    sessionService.cachedRoleName(incoming.getKey()),
                    message.getGroupId(), incoming.getUserId(), true);
            if (role != null && role.matches(text) && contextPointsToBot(message, incoming, text, role.triggerWords())) {
                return AiTriggerDecision.direct(AiChatSettings.mentionWaitMillis(text), "role-trigger");
            }
        }

        if (!AiChatSettings.triggerKeywords().isEmpty()) {
            boolean hit = AiChatSettings.triggerKeywords().stream().anyMatch(word -> StringUtils.isNotBlank(word) && text.contains(word));
            if (hit && contextPointsToBot(message, incoming, text, AiChatSettings.triggerKeywords())) {
                return AiTriggerDecision.direct(AiChatSettings.mentionWaitMillis(text), "keyword");
            }
        }

        int percent = AiChatSettings.randomPercent();
        if (percent > 0 && ThreadLocalRandom.current().nextInt(100) < percent) {
            return AiTriggerDecision.random(AiChatSettings.debounceWaitMillis());
        }
        return AiTriggerDecision.no("ambient");
    }

    /**
     * 触发词命中后的轻量语境判定：不额外调用模型。
     */
    private boolean contextPointsToBot(Message message, AiIncoming incoming, String text, List<String> triggerWords) {
        if (!AiChatSettings.contextJudge()) {
            return true;
        }
        String content = text;
        // URL/代码块里的词不算
        String stripped = CODE.matcher(URL.matcher(content).replaceAll(" ")).replaceAll(" ");
        boolean inPlainText = triggerWords.stream().anyMatch(word -> StringUtils.isNotBlank(word) && stripped.contains(word));
        if (!inPlainText) {
            return false;
        }
        // @了别人且没有 @ 机器人
        if (message.isAtMsg() && !message.isAtBot()) {
            return false;
        }
        boolean address = AiChatSettings.addressWords().stream()
                .filter(StringUtils::isNotBlank)
                .anyMatch(stripped::contains);
        String trimmed = stripped.trim();
        boolean startsWithTrigger = false;
        for (String word : triggerWords) {
            if (StringUtils.isNotBlank(word) && trimmed.startsWith(word)) {
                startsWithTrigger = true;
                break;
            }
        }
        if (THIRD_PARTY.matcher(content).find() && !startsWithTrigger) {
            return false;
        }
        return address || startsWithTrigger;
    }

    private boolean isReplyToBot(Bot bot, AiIncoming incoming) {
        String replyId = incoming.getReplyMessageId();
        if (StringUtils.isBlank(replyId) || bot == null) {
            return false;
        }
        try {
            SyncResponse<Message> response = bot.getMsg(replyId, 2000);
            if (response == null || !response.isSuccess() || response.getData() == null) {
                return false;
            }
            Long sender = response.getData().getUserId();
            return sender != null && sender.equals(bot.getId());
        } catch (Exception e) {
            log.debug("查询引用消息失败: {}", replyId, e);
            return false;
        }
    }
}
