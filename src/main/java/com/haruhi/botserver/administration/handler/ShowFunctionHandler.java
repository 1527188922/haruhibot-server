package com.haruhi.botserver.administration.handler;

import com.haruhi.botserver.bot.handler.IAllMessageHandler;
import com.haruhi.botserver.bot.handler.IMessageHandler;

import com.haruhi.botserver.bot.handler.HandlerWeightEnum;
import com.haruhi.botserver.shared.constant.RegexEnum;
import com.haruhi.botserver.bot.dispatch.MessageDispatcher;
import com.haruhi.botserver.integration.onebot.model.Message;
import com.haruhi.botserver.infrastructure.concurrent.ThreadPoolUtil;
import com.haruhi.botserver.bot.session.Bot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.ObjectProvider;

@Slf4j
@Component
public class ShowFunctionHandler implements IAllMessageHandler {
    private final ObjectProvider<MessageDispatcher> dispatcherProvider;

    public ShowFunctionHandler(ObjectProvider<MessageDispatcher> dispatcherProvider) {
        this.dispatcherProvider = dispatcherProvider;
    }

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
            for (IMessageHandler eventType : dispatcherProvider.getObject().getContainer()) {
                stringBuilder.append("id：").append(eventType.weight()).append("\n");
                stringBuilder.append("名称：").append(eventType.funName()).append("\n");
            }
            bot.sendMessage(message.getUserId(),message.getGroupId(),message.getMessageType(),stringBuilder.toString(),true);
        });
        return true;
    }

}
