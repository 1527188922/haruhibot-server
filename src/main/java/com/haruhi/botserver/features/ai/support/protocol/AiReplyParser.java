package com.haruhi.botserver.features.ai.support.protocol;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.haruhi.botserver.features.ai.model.AiReply;
import com.haruhi.botserver.features.ai.model.AiReplyMessage;
import com.haruhi.botserver.features.ai.model.AiToolCall;
import com.haruhi.botserver.features.ai.support.AiChatSettings;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 解析模型输出的 JSON 协议。对字段宽容、对类型严格。
 */
public final class AiReplyParser {

    private AiReplyParser() {
    }

    public static AiReply parse(String content) {
        if (StringUtils.isBlank(content)) {
            return AiReply.invalid("empty_content");
        }
        String json = extractJson(content);
        if (StringUtils.isBlank(json)) {
            return AiReply.invalid("no_json_object");
        }
        JSONObject root;
        try {
            root = JSON.parseObject(json);
        } catch (Exception e) {
            return AiReply.invalid("invalid_json");
        }
        if (root == null) {
            return AiReply.invalid("invalid_json");
        }

        String type = root.getString("type");
        JSONObject call = firstObject(root, "call");
        if (call == null && root.get("tool") != null) {
            call = new JSONObject(true);
            call.put("name", root.get("tool"));
            call.put("args", root.get("arguments"));
        }
        JSONArray messages = firstArray(root, "messages");

        if ("silent".equalsIgnoreCase(type) || Boolean.TRUE.equals(root.getBoolean("silent"))) {
            return AiReply.silent();
        }
        if (call != null || "call".equalsIgnoreCase(type)) {
            if (call == null) {
                return AiReply.invalid("call_missing");
            }
            AiToolCall toolCall = new AiToolCall();
            toolCall.setName(call.getString("name"));
            Object args = call.get("args");
            if (!(args instanceof JSONObject)) {
                args = call.get("arguments");
            }
            toolCall.setArgs(args instanceof JSONObject object ? object : new JSONObject(true));
            if (StringUtils.isBlank(toolCall.getName())) {
                return AiReply.invalid("call_name_missing");
            }
            return AiReply.call(toolCall);
        }
        if ("say".equalsIgnoreCase(type) || messages != null) {
            List<AiReplyMessage> result = parseMessages(messages, root);
            if (result.isEmpty()) {
                return AiReply.invalid("say_empty");
            }
            return AiReply.say(result);
        }
        return AiReply.invalid("unknown_type");
    }

    private static List<AiReplyMessage> parseMessages(JSONArray array, JSONObject root) {
        List<AiReplyMessage> result = new ArrayList<>();
        int max = AiChatSettings.messageMaxCount();
        if (array == null) {
            // 允许 {"type":"say","text":"..."} 这种简化写法
            String text = root.getString("text");
            if (StringUtils.isNotBlank(text)) {
                AiReplyMessage message = new AiReplyMessage();
                message.setText(text);
                result.add(message);
            }
            return result;
        }
        for (int i = 0; i < array.size() && result.size() < max; i++) {
            Object item = array.get(i);
            if (item instanceof String text) {
                if (StringUtils.isNotBlank(text)) {
                    AiReplyMessage message = new AiReplyMessage();
                    message.setText(text);
                    result.add(message);
                }
                continue;
            }
            if (!(item instanceof JSONObject object)) {
                continue;
            }
            String text = object.getString("text");
            if (StringUtils.isBlank(text)) {
                continue;
            }
            AiReplyMessage message = new AiReplyMessage();
            message.setText(text);
            message.setDelayMs(parseDelay(object));
            message.setReplyTo(StringUtils.defaultIfBlank(object.getString("reply_to"), object.getString("replyTo")));
            message.setAt(StringUtils.defaultIfBlank(object.getString("at"), object.getString("at_qq")));
            result.add(message);
        }
        return result;
    }

    private static int parseDelay(JSONObject object) {
        Object value = object.get("delay_ms");
        if (value == null) {
            value = object.get("delayMs");
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception e) {
            return 0;
        }
    }

    private static JSONObject firstObject(JSONObject root, String key) {
        Object value = root.get(key);
        return value instanceof JSONObject object ? object : null;
    }

    private static JSONArray firstArray(JSONObject root, String key) {
        Object value = root.get(key);
        return value instanceof JSONArray array ? array : null;
    }

    /**
     * 去掉 BOM/代码块，截取第一个 { 到最后一个 }。
     */
    static String extractJson(String content) {
        String text = content.trim();
        if (text.startsWith("\uFEFF")) {
            text = text.substring(1);
        }
        if (text.startsWith("```")) {
            int firstLine = text.indexOf('\n');
            if (firstLine > 0) {
                text = text.substring(firstLine + 1);
            }
            int end = text.lastIndexOf("```");
            if (end >= 0) {
                text = text.substring(0, end);
            }
        }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start < 0 || end <= start) {
            return null;
        }
        return text.substring(start, end + 1);
    }
}
