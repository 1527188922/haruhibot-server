package com.haruhi.botServer.handlers.message;

import com.haruhi.botServer.constant.HandlerWeightEnum;
import com.haruhi.botServer.constant.RegexEnum;
import com.haruhi.botServer.dispenser.MessageDispenser;
import com.haruhi.botServer.dto.gocq.response.Message;
import com.haruhi.botServer.event.message.IAllMessageEvent;
import com.haruhi.botServer.event.message.IMessageEvent;
import com.haruhi.botServer.utils.ThreadPoolUtil;
import com.haruhi.botServer.ws.Bot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ShowFunctionHandler implements IAllMessageEvent {

    @Override
    public int weight() {
        return HandlerWeightEnum.W_860.getWeight();
    }

    @Override
    public String funName() {
        return HandlerWeightEnum.W_860.getName();
    }
    @Override
    public boolean onMessage(Bot bot, Message message) {
        if (!message.getRawMessage().matches(RegexEnum.SHOW_ALL_FUNCTION.getValue())) {
            return false;
        }
        ThreadPoolUtil.getHandleCommandPool().execute(()->{
            StringBuilder stringBuilder = new StringBuilder("所有功能：\n");
            for (IMessageEvent eventType : MessageDispenser.getContainer()) {
                stringBuilder.append("id：").append(eventType.weight()).append("\n");
                stringBuilder.append("名称：").append(eventType.funName()).append("\n");
            }
            bot.sendMessage(message.getUserId(),message.getGroupId(),message.getMessageType(),stringBuilder.toString(),true);
        });
        return true;
    }

}
