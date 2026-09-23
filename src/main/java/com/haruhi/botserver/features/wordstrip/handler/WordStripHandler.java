package com.haruhi.botserver.features.wordstrip.handler;

import com.haruhi.botserver.bot.handler.HandlerWeightEnum;
import com.haruhi.botserver.integration.onebot.model.Message;
import com.haruhi.botserver.bot.handler.IGroupMessageHandler;
import com.haruhi.botserver.infrastructure.concurrent.ThreadPoolUtil;
import com.haruhi.botserver.bot.session.Bot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class WordStripHandler implements IGroupMessageHandler {

    private static Map<String,String> cache = new ConcurrentHashMap<>();
    public static void putCache(Long selfId,Long groupId,String keyWord,String answer){
        cache.put(getKey(selfId,groupId,keyWord),answer);
    }
    public static void clearCache(){
        cache.clear();
    }
    public static void removeCache(Long selfId,Long groupId,String keyWord){
        cache.remove(getKey(selfId,groupId,keyWord));
    }
    public static String getKey(Long selfId,Long groupId,String keyWord){
        return selfId + "-" + groupId + "-" + keyWord;
    }


    @Override
    public int weight() {
        return HandlerWeightEnum.W_680.getWeight();
    }

    @Override
    public String funName() {
        return HandlerWeightEnum.W_680.getName();
    }

    @Override
    public boolean onGroup(Bot bot, final Message message) {
        String answer = cache.get(getKey(message.getSelfId(),message.getGroupId(),message.getRawMessage()));
        if(answer == null){
            return false;
        }
        ThreadPoolUtil.getHandleCommandPool().execute(()->{
            bot.sendGroupMessage(message.getGroupId(),answer,false);
        });
        return true;
    }
}
