package com.haruhi.botserver.features.ai.support.protocol;

import com.haruhi.botserver.features.ai.model.AiReplyMessage;
import com.haruhi.botserver.features.ai.model.AiSessionKey;
import com.haruhi.botserver.features.ai.support.AiChatSettings;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 模型输出的文本清洗：去 Markdown/CQ/内心戏，切分超长消息。
 */
@Slf4j
public final class AiReplySanitizer {

    private static final Pattern CQ = Pattern.compile("\\[CQ:[^\\]]*]");
    private static final Pattern CONTROL = Pattern.compile("[\\p{Cntrl}&&[^\\n\\t]]");
    private static final Pattern MARKDOWN = Pattern.compile("(\\*\\*|__|~~|`)");
    private static final Pattern HEADING = Pattern.compile("(?m)^\\s*#{1,6}\\s*");
    private static final Pattern LIST = Pattern.compile("(?m)^\\s*[-*+]\\s+");
    private static final Pattern QUOTE = Pattern.compile("(?m)^\\s*>\\s?");
    private static final Pattern BRACKET_ONLY = Pattern.compile("^[（(].*[）)]$");
    private static final List<String> INTERNAL_MARKERS = List.of(
            "【安全与权限", "【输出协议", "【角色卡：", "【最近群聊", "【本条消息", "【消息开始】"
    );

    private AiReplySanitizer() {
    }

    public static List<AiReplyMessage> clean(List<AiReplyMessage> messages, AiSessionKey key) {
        List<AiReplyMessage> result = new ArrayList<>();
        if (messages == null) {
            return result;
        }
        String previous = null;
        int maxCount = AiChatSettings.messageMaxCount();
        int maxChars = AiChatSettings.messageMaxChars();
        for (AiReplyMessage message : messages) {
            if (message == null || result.size() >= maxCount) {
                break;
            }
            String text = cleanText(message.getText());
            if (StringUtils.isBlank(text)) {
                continue;
            }
            if (text.equals(previous)) {
                continue;
            }
            for (String part : split(text, maxChars)) {
                if (result.size() >= maxCount) {
                    break;
                }
                AiReplyMessage item = new AiReplyMessage();
                item.setText(part);
                item.setDelayMs(message.getDelayMs());
                item.setReplyTo(message.getReplyTo());
                item.setAt(message.getAt());
                result.add(item);
                previous = part;
            }
        }
        return result;
    }

    public static String cleanText(String raw) {
        if (StringUtils.isBlank(raw)) {
            return "";
        }
        String text = raw.replace("\r\n", "\n").replace('\r', '\n');
        text = CONTROL.matcher(text).replaceAll("");
        text = CQ.matcher(text).replaceAll("");
        text = text.replace("@全体成员", "").replace("@all", "");
        text = MARKDOWN.matcher(text).replaceAll("");
        text = HEADING.matcher(text).replaceAll("");
        text = LIST.matcher(text).replaceAll("");
        text = QUOTE.matcher(text).replaceAll("");
        text = text.replaceAll("\\n+", " ").replaceAll("\\s{2,}", " ").trim();
        if (text.isEmpty()) {
            return "";
        }
        if (BRACKET_ONLY.matcher(text).matches()) {
            log.debug("丢弃括号内心戏: {}", text);
            return "";
        }
        for (String marker : INTERNAL_MARKERS) {
            if (text.contains(marker)) {
                log.warn("模型输出命中内部提示词标记，已丢弃: {}", marker);
                return "";
            }
        }
        return text;
    }

    private static List<String> split(String text, int maxChars) {
        List<String> parts = new ArrayList<>();
        if (text.length() <= maxChars) {
            parts.add(text);
            return parts;
        }
        String remaining = text;
        while (remaining.length() > maxChars) {
            int cut = -1;
            for (int i = Math.min(remaining.length() - 1, maxChars); i > maxChars / 2; i--) {
                char c = remaining.charAt(i);
                if (c == '。' || c == '！' || c == '？' || c == '!' || c == '?' || c == '；' || c == ';') {
                    cut = i + 1;
                    break;
                }
            }
            if (cut <= 0) {
                cut = maxChars;
            }
            parts.add(remaining.substring(0, cut).trim());
            remaining = remaining.substring(cut).trim();
        }
        if (!remaining.isBlank()) {
            parts.add(remaining);
        }
        return parts;
    }
}
