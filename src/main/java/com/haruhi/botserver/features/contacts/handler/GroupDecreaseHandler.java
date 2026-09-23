package com.haruhi.botserver.features.contacts.handler;

import com.haruhi.botserver.bot.handler.IGroupDecreaseHandler;

import com.haruhi.botserver.configuration.service.Configs;

import com.haruhi.botserver.configuration.metadata.ConfigKey;

import com.haruhi.botserver.integration.onebot.model.MessageTypeEnum;
import com.haruhi.botserver.integration.onebot.model.Message;
import com.haruhi.botserver.integration.onebot.model.MessageHolder;
import com.haruhi.botserver.infrastructure.concurrent.ThreadPoolUtil;
import com.haruhi.botserver.bot.session.Bot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;

@Slf4j
@Component
public class GroupDecreaseHandler implements IGroupDecreaseHandler {

    @Override
    public void onGroupDecrease(Bot bot, Message message) {
        boolean groupDecrease = Configs.getBool(ConfigKey.BOT_SWITCH_GROUP_DECREASE, false);
        if(!groupDecrease){
            return;
        }
        ThreadPoolUtil.getHandleCommandPool().execute(()->{
            bot.sendMessage(message.getUserId(), message.getGroupId(), MessageTypeEnum.group.getType(),
                    MessageHolder.instanceText(MessageFormat.format("{0} 离开了本群。",String.valueOf(message.getUserId()))));
        });
    }

}
