package com.haruhi.botserver.features.contacts.handler;

import com.haruhi.botserver.bot.handler.IGroupIncreaseHandler;

import com.haruhi.botserver.configuration.service.Configs;

import com.haruhi.botserver.configuration.metadata.ConfigKey;

import com.haruhi.botserver.integration.onebot.model.CqCodeTypeEnum;
import com.haruhi.botserver.integration.onebot.model.Message;
import com.haruhi.botserver.infrastructure.concurrent.ThreadPoolUtil;
import com.haruhi.botserver.bot.session.Bot;
import com.simplerobot.modules.utils.KQCodeUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;

@Slf4j
@Component
public class GroupIncreaseHandler implements IGroupIncreaseHandler {

    @Override
    public void onGroupIncrease(Bot bot, Message message) {

        boolean groupIncrease = Configs.getBool(ConfigKey.BOT_SWITCH_GROUP_INCREASE, false);
        if(!groupIncrease || message.isSelfMsg()){
            return;
        }
        ThreadPoolUtil.getHandleCommandPool().execute(()->{

            KQCodeUtils instance = KQCodeUtils.getInstance();
            String at = instance.toCq(CqCodeTypeEnum.at.getType(), "qq=" + message.getUserId());
            String faces = "";
            String face = instance.toCq(CqCodeTypeEnum.face.getType(), "id=" + 144);
            for (int i = 0; i < 3; i++) {
                faces += face;
            }
            bot.sendGroupMessage(message.getGroupId(), MessageFormat.format("{0} 欢迎小可爱~{1}",at,faces),false);
        });
    }

}
