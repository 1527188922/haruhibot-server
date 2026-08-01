package com.haruhi.botServer.handler.message.image;

import com.alibaba.fastjson.JSONObject;
import com.haruhi.botServer.cache.CacheSet;
import com.haruhi.botServer.dto.qqclient.Message;
import com.haruhi.botServer.dto.qqclient.MessageData;
import com.haruhi.botServer.dto.qqclient.SyncResponse;
import com.haruhi.botServer.ws.Bot;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ImageSearchTriggerResolver {

    private static final int CACHE_SIZE = 20;
    private static final long CACHE_SECONDS = 30;
    private static final long GET_REPLY_TIMEOUT = 2L * 1000L;

    private final CacheSet<String> cache = new CacheSet<>(CACHE_SECONDS, TimeUnit.SECONDS, CACHE_SIZE);

    public ImageSearchMatch resolve(Bot bot, Message message, String regex) {
        ImageSearchMatch replyMatch = resolveReplyImage(bot, message, regex);
        if (replyMatch.isMatched()) {
            return replyMatch;
        }

        List<MessageData> picMessageData = message.getPicMessageData();
        String key = getKey(message);
        if (cache.contains(key) && CollectionUtils.isNotEmpty(picMessageData)) {
            return ImageSearchMatch.ready(picMessageData.getFirst().getUrl(), null, key);
        }

        if (!matches(message, regex)) {
            return ImageSearchMatch.notMatched();
        }

        if (CollectionUtils.isEmpty(picMessageData)) {
            cache.add(key);
            return ImageSearchMatch.waiting(key);
        }
        return ImageSearchMatch.ready(picMessageData.getFirst().getUrl(), null, key);
    }

    public void clear(String key) {
        if (key != null) {
            cache.remove(key);
        }
    }

    private ImageSearchMatch resolveReplyImage(Bot bot, Message message, String regex) {
        if (!message.isReplyMsg() || !message.isTextMsg()) {
            return ImageSearchMatch.notMatched();
        }
        String text = message.getText(0);
        if (text == null || !text.trim().matches(regex)) {
            return ImageSearchMatch.notMatched();
        }
        List<String> replyMsgIds = message.getReplyMsgIds();
        if (CollectionUtils.isEmpty(replyMsgIds)) {
            return ImageSearchMatch.notMatched();
        }
        SyncResponse<Message> response = bot.getMsg(replyMsgIds.getFirst(), GET_REPLY_TIMEOUT);
        Message replyMessage = response != null ? response.getData() : null;
        log.debug("Reply image search messageId:{} msg:{}", replyMsgIds.getFirst(), JSONObject.toJSONString(replyMessage));
        if (replyMessage != null && replyMessage.isPicMsg() && CollectionUtils.isNotEmpty(replyMessage.getPicUrls())) {
            return ImageSearchMatch.ready(replyMessage.getPicUrls().getFirst(), replyMessage, null);
        }
        return ImageSearchMatch.notMatched();
    }

    private boolean matches(Message message, String regex) {
        List<String> texts = message.getTexts();
        if (CollectionUtils.isEmpty(texts)) {
            return false;
        }
        String msg = texts.getFirst().trim();
        String[] split = regex.split("\\|");
        for (String item : split) {
            if (item.equals(msg)) {
                return true;
            }
        }
        return false;
    }

    private String getKey(Message message) {
        return message.getSelfId() + "-" + message.getUserId() + "-" + message.getGroupId();
    }
}
