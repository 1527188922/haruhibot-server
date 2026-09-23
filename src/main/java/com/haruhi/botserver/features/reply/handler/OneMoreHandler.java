package com.haruhi.botserver.features.reply.handler;

import com.haruhi.botserver.bot.handler.IGroupMessageHandler;

import com.haruhi.botserver.bot.handler.HandlerWeightEnum;
import com.haruhi.botserver.integration.onebot.model.Message;
import com.haruhi.botserver.infrastructure.concurrent.ThreadPoolUtil;
import com.haruhi.botserver.bot.session.Bot;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class OneMoreHandler implements IGroupMessageHandler {

    @Override
    public int weight() {
        return HandlerWeightEnum.W_990.getWeight();
    }

    @Override
    public String funName() {
        return HandlerWeightEnum.W_990.getName();
    }

    @Override
    public boolean handleSelfMsg() {
        return true;
    }

    private final ConcurrentHashMap<String, Pair<String,Boolean>> msgCache = new ConcurrentHashMap<>();

    @Override
    public boolean onGroup(Bot bot, Message message) {

        ThreadPoolUtil.getHandleCommandPool().execute(()->{

            String groupIdStr = String.valueOf(message.getGroupId())+message.getSelfId();
            Pair<String, Boolean> pair = msgCache.get(groupIdStr);
            if(pair == null){
                putMsg(message);
                return;
            }

            if (pair.getLeft().equals(message.getRawMessage())) {
                if (!pair.getRight()) {
                    bot.sendMessage(message.getUserId(), message.getGroupId(), message.getMessageType(), message.getMessage());
                    pair.setValue(true);
                }
            }else{
                msgCache.remove(groupIdStr);
                putMsg(message);
            }
        });
        return false;
    }

    private void putMsg(Message message){
        if (!message.isPicMsg()) {
            msgCache.put(String.valueOf(message.getGroupId())+message.getSelfId(), MutablePair.of(message.getRawMessage(),false));
        }
    }

}
