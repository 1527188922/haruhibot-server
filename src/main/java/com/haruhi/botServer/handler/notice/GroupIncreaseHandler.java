package com.haruhi.botServer.handler.notice;

import com.haruhi.botServer.config.config.Configs;

import com.haruhi.botServer.config.config.ConfigKey;

import com.haruhi.botServer.constant.CqCodeTypeEnum;
import com.haruhi.botServer.dto.qqclient.Message;
import com.haruhi.botServer.utils.ThreadPoolUtil;
import com.haruhi.botServer.ws.Bot;
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
