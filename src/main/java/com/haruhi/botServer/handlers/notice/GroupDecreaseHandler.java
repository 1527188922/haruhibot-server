package com.haruhi.botServer.handlers.notice;

import com.haruhi.botServer.constant.event.MessageTypeEnum;
import com.haruhi.botServer.dto.qqclient.Message;
import com.haruhi.botServer.dto.qqclient.MessageHolder;
import com.haruhi.botServer.event.notice.IGroupDecreaseEvent;
import com.haruhi.botServer.service.DictionarySqliteService;
import com.haruhi.botServer.utils.ThreadPoolUtil;
import com.haruhi.botServer.ws.Bot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;

@Slf4j
@Component
public class GroupDecreaseHandler implements IGroupDecreaseEvent {

    @Autowired
    private DictionarySqliteService dictionarySqliteService;

    @Override
    public void onGroupDecrease(Bot bot, Message message) {
        boolean groupDecrease = dictionarySqliteService.getBoolean(DictionarySqliteService.DictionaryEnum.SWITCH_GROUP_DECREASE.getKey(), false);
        if(!groupDecrease){
            return;
        }
        ThreadPoolUtil.getHandleCommandPool().execute(()->{
            bot.sendMessage(message.getUserId(), message.getGroupId(), MessageTypeEnum.group.getType(),
                    MessageHolder.instanceText(MessageFormat.format("{0} 离开了本群。",String.valueOf(message.getUserId()))));
        });
    }
}
