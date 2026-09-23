package com.haruhi.botserver.administration.handler;

import com.haruhi.botserver.bot.handler.IPrivateMessageHandler;

import com.haruhi.botserver.shared.annotation.SuperuserAuthentication;
import com.haruhi.botserver.bot.handler.HandlerWeightEnum;
import com.haruhi.botserver.shared.constant.RegexEnum;
import com.haruhi.botserver.integration.onebot.model.Message;
import com.haruhi.botserver.integration.onebot.model.MessageHolder;
import com.haruhi.botserver.shared.error.BusinessException;
import com.haruhi.botserver.administration.service.SystemService;
import com.haruhi.botserver.infrastructure.concurrent.ThreadPoolUtil;
import com.haruhi.botserver.bot.session.Bot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Component
@Slf4j
public class RestartBotHandler implements IPrivateMessageHandler {

    @Override
    public int weight() {
        return HandlerWeightEnum.W_190.getWeight();
    }

    @Override
    public String funName() {
        return HandlerWeightEnum.W_190.getName();
    }

    @Autowired
    private SystemService systemService;

    @SuperuserAuthentication
    @Override
    public boolean onPrivate(Bot bot, Message message) {
        if (!(message.isTextMsg() && message.getText(-1).matches(RegexEnum.RESTART.getValue()))){
            return false;
        }

        ThreadPoolUtil.getHandleCommandPool().execute(()->{
            try {
                systemService.restartBot();
            }catch (BusinessException e){
                bot.sendMessage(message.getUserId(), message.getGroupId(), message.getMessageType(), MessageHolder.instanceText(e.getErrorMsg()));
                return;
            }
            bot.sendMessage(message.getUserId(), message.getGroupId(), message.getMessageType(), MessageHolder.instanceText("重启命令已执行"));
        });
        return true;
    }

}
