package com.haruhi.botserver.features.contacts.handler;

import com.haruhi.botserver.bot.handler.INoticeHandler;

import com.haruhi.botserver.bot.session.Bot;
import com.haruhi.botserver.configuration.metadata.ConfigKey;
import com.haruhi.botserver.configuration.service.Configs;
import com.haruhi.botserver.integration.onebot.model.MessageHolder;
import com.haruhi.botserver.integration.onebot.model.NoticeTypeEnum;
import com.haruhi.botserver.integration.onebot.model.event.NoticeEvent;
import com.haruhi.botserver.shared.util.CommonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;
import java.util.List;

@Slf4j
@Component
public class GroupDecreaseHandler implements INoticeHandler {

    @Override
    public boolean support(NoticeEvent event) {
        return NoticeTypeEnum.group_decrease.name().equals(event.getNoticeType());
    }

    @Override
    public void onNotice(Bot bot, NoticeEvent event) {
        boolean groupDecrease = Configs.getBool(ConfigKey.BOT_SWITCH_GROUP_DECREASE, false);
        if(!groupDecrease){
            return;
        }
        List<MessageHolder> messageHolders = MessageHolder.instanceText(MessageFormat.format("{0} 离开了本群。", String.valueOf(event.getUserId())));
        messageHolders.add(MessageHolder.instanceImage(CommonUtil.getAvatarUrl(event.getUserId(), false)));

        bot.sendGroupMessage(event.getGroupId(), messageHolders);
    }

}
