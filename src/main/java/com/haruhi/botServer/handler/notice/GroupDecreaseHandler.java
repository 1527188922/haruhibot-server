package com.haruhi.botServer.handler.notice;

import com.haruhi.botServer.config.config.Configs;

import com.haruhi.botServer.config.config.ConfigKey;

import com.haruhi.botServer.constant.event.MessageTypeEnum;
import com.haruhi.botServer.dto.qqclient.Message;
import com.haruhi.botServer.dto.qqclient.MessageHolder;
import com.haruhi.botServer.utils.ThreadPoolUtil;
import com.haruhi.botServer.ws.Bot;
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
