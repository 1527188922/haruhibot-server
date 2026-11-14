package com.haruhi.botserver.features.ai.support;

import com.haruhi.botserver.integration.onebot.model.Message;
import com.haruhi.botserver.integration.onebot.model.MessageData;
import com.haruhi.botserver.integration.onebot.model.MessageHolder;
import com.haruhi.botserver.integration.onebot.model.MessageHolderTypeEnum;
import com.haruhi.botserver.integration.onebot.model.Sender;
import com.haruhi.botserver.features.ai.model.AiIncoming;
import com.haruhi.botserver.features.ai.model.AiSessionKey;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * 把 OneBot Message 提取成模型可读的入站消息。
 */
public final class AiMessageExtractor {

    private AiMessageExtractor() {
    }

    public static AiIncoming extract(Message message) {
        AiIncoming incoming = new AiIncoming();
        incoming.setKey(AiSessionKey.of(message));
        incoming.setUserId(message.getUserId() == null ? 0L : message.getUserId());
        incoming.setMessageId(message.getMessageId());
        if (StringUtils.isNotBlank(message.getMessageId())) {
            incoming.getMessageIds().add(message.getMessageId());
        }
        incoming.setTimestamp(message.getTime() == null ? System.currentTimeMillis() : message.getTime() * 1000L);
        incoming.setRawMessage(message.getRawMessage());
        incoming.setAtBot(message.isAtBot());
        incoming.setHasImage(message.isPicMsg());

        Sender sender = message.getSender();
        incoming.setUserName(displayName(sender, incoming.getUserId()));

        List<String> replyIds = message.getReplyMsgIds();
        if (!replyIds.isEmpty()) {
            incoming.setReplyMessageId(replyIds.get(0));
        }

        String plainText = message.getText(-1);
        boolean atOnly = message.isAtBot()
                && StringUtils.isBlank(plainText)
                && !message.isPicMsg()
                && !hasOtherMedia(message);
        incoming.setAtOnly(atOnly);
        incoming.setText(atOnly ? "" : buildText(message));
        return incoming;
    }

    private static String buildText(Message message) {
        if (message.getMessage() == null || message.getMessage().isEmpty()) {
            return StringUtils.defaultString(message.getRawMessage());
        }
        StringBuilder sb = new StringBuilder();
        for (MessageHolder holder : message.getMessage()) {
            if (holder == null || holder.getType() == null) {
                continue;
            }
            MessageData data = holder.getData();
            String type = holder.getType();
            if (MessageHolderTypeEnum.text.name().equals(type)) {
                if (data != null && data.getText() != null) {
                    sb.append(data.getText());
                }
            } else if (MessageHolderTypeEnum.image.name().equals(type)) {
                sb.append("[图片]");
            } else if (MessageHolderTypeEnum.face.name().equals(type)) {
                sb.append("[表情]");
            } else if (MessageHolderTypeEnum.record.name().equals(type)) {
                sb.append("[语音]");
            } else if (MessageHolderTypeEnum.video.name().equals(type)) {
                sb.append("[视频]");
            } else if (MessageHolderTypeEnum.at.name().equals(type)) {
                String qq = data == null ? null : data.getQq();
                if ("all".equals(qq)) {
                    sb.append("@全体成员");
                } else if (qq != null && qq.equals(String.valueOf(message.getSelfId()))) {
                    sb.append("@你 ");
                } else {
                    String name = data == null ? null : data.getName();
                    sb.append("@").append(StringUtils.defaultIfBlank(name, qq)).append(' ');
                }
            } else if (MessageHolderTypeEnum.json.name().equals(type)) {
                sb.append("[卡片]");
            } else if (MessageHolderTypeEnum.music.name().equals(type)) {
                sb.append("[音乐]");
            }
        }
        String text = sb.toString().trim();
        return text.isBlank() ? StringUtils.defaultString(message.getRawMessage()) : text;
    }

    private static boolean hasOtherMedia(Message message) {
        if (message.getMessage() == null) {
            return false;
        }
        for (MessageHolder holder : message.getMessage()) {
            if (holder == null || holder.getType() == null) {
                continue;
            }
            String type = holder.getType();
            if (MessageHolderTypeEnum.text.name().equals(type) || MessageHolderTypeEnum.at.name().equals(type)) {
                continue;
            }
            return true;
        }
        return false;
    }

    public static String displayName(Sender sender, long userId) {
        if (sender != null) {
            if (StringUtils.isNotBlank(sender.getCard())) {
                return sender.getCard();
            }
            if (StringUtils.isNotBlank(sender.getNickname())) {
                return sender.getNickname();
            }
        }
        return userId == 0 ? "群友" : String.valueOf(userId);
    }
}
