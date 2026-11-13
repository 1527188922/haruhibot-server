package com.haruhi.botserver.features.contacts.handler;

import com.haruhi.botserver.bot.handler.DebounceParam;
import com.haruhi.botserver.bot.handler.IDebounceableNoticeHandler;

import com.haruhi.botserver.bot.session.Bot;
import com.haruhi.botserver.configuration.metadata.ConfigKey;
import com.haruhi.botserver.configuration.service.Configs;
import com.haruhi.botserver.integration.onebot.model.MessageHolder;
import com.haruhi.botserver.integration.onebot.model.NoticeTypeEnum;
import com.haruhi.botserver.integration.onebot.model.event.NoticeEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class GroupIncreaseHandler implements IDebounceableNoticeHandler {

    /**
     * 入群通知防抖窗口：同一条入群通知可能被上报多次，窗口内只处理第一条
     */
    private static final long DEBOUNCE_WINDOW_MILLIS = TimeUnit.SECONDS.toMillis(10);

    @Override
    public boolean support(NoticeEvent event) {
        return NoticeTypeEnum.group_increase.name().equals(event.getNoticeType());
    }

    @Override
    public DebounceParam debounceParam(NoticeEvent event) {
        return DebounceParam.of(event, DEBOUNCE_WINDOW_MILLIS);
    }

    @Override
    public void onNotice(Bot bot, NoticeEvent event) {

        boolean groupIncrease = Configs.getBool(ConfigKey.BOT_SWITCH_GROUP_INCREASE, false);
        if(!groupIncrease || event.isSelfMsg()){
            return;
        }
        List<MessageHolder> messageHolders = MessageHolder.instanceAt(String.valueOf(event.getUserId()));
        messageHolders.addAll(MessageHolder.instanceText(" 欢迎小可爱~"));
        messageHolders.addAll(MessageHolder.instanceFace("144", 3));
        bot.sendGroupMessage(event.getGroupId(), messageHolders);
    }

}
