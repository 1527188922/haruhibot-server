package com.haruhi.botServer.handlers.notice;

import com.haruhi.botServer.config.SwitchConfig;
import com.haruhi.botServer.dto.gocq.response.Message;
import com.haruhi.botServer.event.notice.IGroupDecreaseEvent;
import com.haruhi.botServer.utils.ThreadPoolUtil;
import com.haruhi.botServer.ws.Bot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;

@Slf4j
@Component
public class GroupDecreaseHandler implements IGroupDecreaseEvent {


    @Override
    public void onGroupDecrease(final Bot bot, final Message message) {
        if(!SwitchConfig.GROUP_DECREASE){
            return;
        }
        ThreadPoolUtil.getHandleCommandPool().execute(()->{
            bot.sendGroupMessage(message.getGroupId(), MessageFormat.format("{0} 离开了本群。",String.valueOf(message.getUserId())), true);
        });
    }
}
