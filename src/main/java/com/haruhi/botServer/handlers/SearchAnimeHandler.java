package com.haruhi.botServer.handlers;

import com.alibaba.fastjson.JSONObject;
import com.haruhi.botServer.cache.CacheSet;
import com.haruhi.botServer.constant.HandlerWeightEnum;
import com.haruhi.botServer.constant.RegexEnum;
import com.haruhi.botServer.dto.qqclient.Message;
import com.haruhi.botServer.dto.qqclient.MessageData;
import com.haruhi.botServer.event.message.IAllMessageEvent;
import com.haruhi.botServer.utils.ThreadPoolUtil;
import com.haruhi.botServer.ws.Bot;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 根据图片搜索番剧
 */
@Slf4j
//@Component
public class SearchAnimeHandler implements IAllMessageEvent {

    private static int size = 20;

    private static CacheSet<String> cache = new CacheSet<>(30, TimeUnit.SECONDS,size);

    private String getKey(String selfId,String userId,String groupId){
        return  selfId + "-" + userId + "-" + groupId;
    }

    @Override
    public int weight() {
        return HandlerWeightEnum.W_750.getWeight();
    }

    @Override
    public String funName() {
        return HandlerWeightEnum.W_750.getName();
    }


    @Override
    public boolean onMessage(Bot bot, Message message) {
        Message replyMessage = replySearch(bot, message);
        if(replyMessage != null){
            startSearch(bot,message,replyMessage,replyMessage.getPicUrls().get(0),null);
            return true;
        }

        List<MessageData> picMessageData = message.getPicMessageData();
        String key = getKey(String.valueOf(message.getSelfId()), String.valueOf(message.getUserId()), String.valueOf(message.getGroupId()));
        if(cache.contains(key) && CollectionUtils.isNotEmpty(picMessageData)){
            // 存在缓存 并且 图片不为空
            startSearch(bot,message,null,picMessageData.get(0).getUrl(),key);
            return true;
        }
        boolean matches = matches(message);

        if (matches) {
            if(CollectionUtils.isEmpty(picMessageData)){
                cache.add(key);
                bot.sendMessage(message.getUserId(),message.getGroupId(),message.getMessageType(),"图呢！",true);
            }else {
                startSearch(bot,message,null,picMessageData.get(0).getUrl(),key);
            }
            return true;
        }
        return false;
    }


    private void startSearch(Bot bot,Message message, Message replyMessage,String imgUrl, String key){
        bot.sendMessage(message.getUserId(),message.getGroupId(),message.getMessageType(),"开始搜番...",true);
        ThreadPoolUtil.getHandleCommandPool().execute(()->{


        });
        if(key != null){
            cache.remove(key);
        }
    }


    private Message replySearch(Bot bot, Message message){
        if (message.isGroupMsg() && message.isReplyMsg() && message.isTextMsg()) {
            if (message.getText(0).trim().matches(RegexEnum.SEARCH_ANIME.getValue())) {
                List<String> replyMsgIds = message.getReplyMsgIds();
                Message msg = bot.getMsg(replyMsgIds.get(0),2L * 1000L).getData();
                log.debug("回复式识番，根据msgId获取消息 {} {}",replyMsgIds.get(0), JSONObject.toJSONString(msg));
                if(msg != null && msg.isPicMsg()){
                    return msg;
                }
            }
        }
        return null;
    }


    private boolean matches(Message message){
        List<String> texts = message.getTexts();
        if(CollectionUtils.isEmpty(texts)){
            return false;
        }
        String msg = texts.get(0).trim();
        String[] split = RegexEnum.SEARCH_ANIME.getValue().split("\\|");
        for (String s : split) {
            if(s.equals(msg)){
                return true;
            }
        }
        return false;
    }
}
