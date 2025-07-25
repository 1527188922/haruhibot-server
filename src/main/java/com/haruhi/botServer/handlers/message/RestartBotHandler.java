package com.haruhi.botServer.handlers.message;

import com.haruhi.botServer.annotation.SuperuserAuthentication;
import com.haruhi.botServer.constant.HandlerWeightEnum;
import com.haruhi.botServer.constant.RegexEnum;
import com.haruhi.botServer.dto.qqclient.Message;
import com.haruhi.botServer.dto.qqclient.MessageHolder;
import com.haruhi.botServer.event.message.IPrivateMessageEvent;
import com.haruhi.botServer.exception.BusinessException;
import com.haruhi.botServer.service.SystemService;
import com.haruhi.botServer.utils.ThreadPoolUtil;
import com.haruhi.botServer.ws.Bot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Component
@Slf4j
public class RestartBotHandler implements IPrivateMessageEvent {

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
        if (!message.getText(-1).matches(RegexEnum.RESTART.getValue())){
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
