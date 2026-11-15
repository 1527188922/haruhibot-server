package com.haruhi.botserver.features.ai.service;

import com.haruhi.botserver.features.ai.model.AiAmbientMessage;
import com.haruhi.botserver.features.ai.model.AiIncoming;
import com.haruhi.botserver.features.ai.model.AiSessionKey;
import com.haruhi.botserver.features.ai.support.AiChatSettings;
import com.haruhi.botserver.features.ai.support.AiMessageExtractor;
import com.haruhi.botserver.features.chatrecord.persistence.entity.ChatRecordGroup;
import com.haruhi.botserver.features.chatrecord.service.ChatRecordService;
import com.haruhi.botserver.integration.onebot.model.Message;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 群聊环境上下文：只放内存，不落库；内存不足时从聊天记录回填最近消息，作为不进模型历史的“背景音”。
 */
@Service
@Slf4j
public class AiContextService {

    private static final long EXPIRE_MILLIS = 30 * 60 * 1000L;
    private static final Pattern CQ = Pattern.compile("\\[CQ:([a-zA-Z0-9_-]+)((?:,[^\\]]*)?)]");
    private static final DateTimeFormatter RECORD_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final Map<String, GroupBuffer> buffers = new ConcurrentHashMap<>();
    private final ChatRecordService chatRecordService;

    public AiContextService(ChatRecordService chatRecordService) {
        this.chatRecordService = chatRecordService;
    }

    public void record(Message message) {
        if (message == null || !message.isGroupMsg() || message.isSelfMsg()) {
            return;
        }
        if (message.getGroupId() == null || !AiChatSettings.groupEnabled(message.getGroupId())) {
            return;
        }
        AiIncoming incoming = AiMessageExtractor.extract(message);
        if (incoming.getText() == null || incoming.getText().isBlank()) {
            return;
        }
        String key = AiSessionKey.of(message).cacheKey();
        GroupBuffer buffer = buffers.computeIfAbsent(key, ignored -> new GroupBuffer());
        synchronized (buffer) {
            buffer.messages.addLast(new AiAmbientMessage(
                    incoming.getMessageId(),
                    incoming.getUserId(),
                    incoming.getUserName(),
                    incoming.getText(),
                    incoming.getTimestamp()));
            int max = Math.max(4, AiChatSettings.ambientSize() * 2);
            while (buffer.messages.size() > max) {
                buffer.messages.removeFirst();
            }
            buffer.lastAccess = System.currentTimeMillis();
        }
    }

    public List<AiAmbientMessage> recent(AiSessionKey key, int limit, Collection<String> excludeMessageIds) {
        if (key == null || !key.group() || limit <= 0) {
            return List.of();
        }
        List<AiAmbientMessage> memory = snapshot(key, excludeMessageIds);
        if (memory.size() >= limit) {
            return new ArrayList<>(memory.subList(memory.size() - limit, memory.size()));
        }
        List<AiAmbientMessage> merged = new ArrayList<>(backfill(key, limit, excludeMessageIds));
        merged.addAll(memory);
        Map<String, AiAmbientMessage> deduped = new LinkedHashMap<>();
        for (AiAmbientMessage message : merged) {
            String id = StringUtils.isNotBlank(message.getMessageId())
                    ? message.getMessageId()
                    : "noid:" + message.getTimestamp() + ":" + message.getSpeakerId() + ":" + message.getText().hashCode();
            deduped.put(id, message);
        }
        List<AiAmbientMessage> result = new ArrayList<>(deduped.values());
        result.sort(Comparator.comparingLong(AiAmbientMessage::getTimestamp));
        return result.size() <= limit ? result : new ArrayList<>(result.subList(result.size() - limit, result.size()));
    }

    private List<AiAmbientMessage> snapshot(AiSessionKey key, Collection<String> excludeMessageIds) {
        GroupBuffer buffer = buffers.get(key.cacheKey());
        if (buffer == null) {
            return List.of();
        }
        synchronized (buffer) {
            if (System.currentTimeMillis() - buffer.lastAccess > EXPIRE_MILLIS) {
                buffers.remove(key.cacheKey());
                return List.of();
            }
            List<AiAmbientMessage> result = new ArrayList<>(buffer.messages.size());
            for (AiAmbientMessage message : buffer.messages) {
                if (excludeMessageIds != null && excludeMessageIds.contains(message.getMessageId())) {
                    continue;
                }
                result.add(message);
            }
            return result;
        }
    }

    /**
     * 从聊天记录回填最近消息，供重启/冷启动后的第一轮使用。
     */
    private List<AiAmbientMessage> backfill(AiSessionKey key, int limit, Collection<String> excludeMessageIds) {
        List<ChatRecordGroup> records;
        try {
            records = chatRecordService.recentGroupMessages(key.targetId(), Math.min(50, Math.max(limit, 1) * 2));
        } catch (Exception e) {
            log.warn("回填群聊上下文失败: group={}", key.targetId(), e);
            return List.of();
        }
        if (records.isEmpty()) {
            return List.of();
        }
        long cutoff = System.currentTimeMillis() - EXPIRE_MILLIS;
        List<AiAmbientMessage> result = new ArrayList<>();
        for (ChatRecordGroup record : records) {
            if (record == null) {
                continue;
            }
            if (record.getDeleted() != null && record.getDeleted() != 0) {
                continue;
            }
            if (record.getUserId() != null && record.getUserId().equals(record.getSelfId())) {
                continue;
            }
            String messageId = record.getMessageId();
            if (excludeMessageIds != null && messageId != null && excludeMessageIds.contains(messageId)) {
                continue;
            }
            String text = plainText(record.getContent(), record.getSelfId() == null ? 0L : record.getSelfId());
            if (StringUtils.isBlank(text)) {
                continue;
            }
            long timestamp = parseTime(record.getTime());
            if (timestamp > 0 && timestamp < cutoff) {
                continue;
            }
            result.add(new AiAmbientMessage(
                    messageId,
                    record.getUserId() == null ? 0L : record.getUserId(),
                    speakerName(record),
                    text,
                    timestamp));
        }
        return result;
    }

    private String speakerName(ChatRecordGroup record) {
        String name = StringUtils.defaultIfBlank(record.getCard(), record.getNickname());
        if (StringUtils.isBlank(name)) {
            name = record.getUserId() == null ? "群友" : String.valueOf(record.getUserId());
        }
        return name;
    }

    private long parseTime(String time) {
        if (StringUtils.isBlank(time)) {
            return 0L;
        }
        try {
            return LocalDateTime.parse(time.trim(), RECORD_TIME)
                    .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        } catch (Exception e) {
            return 0L;
        }
    }

    /**
     * 把聊天记录里的原始 CQ 码转成模型可读文本，与实时提取的占位符保持一致。
     */
    private String plainText(String raw, long selfId) {
        if (StringUtils.isBlank(raw)) {
            return "";
        }
        Matcher matcher = CQ.matcher(raw);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String type = matcher.group(1);
            String params = matcher.group(2) == null ? "" : matcher.group(2);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(cqText(type, params, selfId)));
        }
        matcher.appendTail(sb);
        return sb.toString().replaceAll("\\s+", " ").trim();
    }

    private String cqText(String type, String params, long selfId) {
        if (type == null) {
            return "";
        }
        return switch (type) {
            case "image" -> "[图片]";
            case "face", "mface" -> "[表情]";
            case "record" -> "[语音]";
            case "video" -> "[视频]";
            case "json" -> "[卡片]";
            case "music" -> "[音乐]";
            case "file" -> "[文件]";
            case "at" -> atText(params, selfId);
            default -> "";
        };
    }

    private String atText(String params, long selfId) {
        String qq = param(params, "qq");
        if ("all".equals(qq)) {
            return "@全体成员 ";
        }
        if (qq != null && qq.equals(String.valueOf(selfId))) {
            return "@你 ";
        }
        String name = StringUtils.defaultIfBlank(param(params, "name"), qq);
        return StringUtils.isBlank(name) ? "@某人 " : "@" + name + " ";
    }

    private String param(String params, String key) {
        for (String part : params.split(",")) {
            int index = part.indexOf('=');
            if (index > 0 && key.equals(part.substring(0, index).trim())) {
                return part.substring(index + 1);
            }
        }
        return null;
    }

    private static class GroupBuffer {
        private final Deque<AiAmbientMessage> messages = new ArrayDeque<>();
        private volatile long lastAccess = System.currentTimeMillis();
    }
}
