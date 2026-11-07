package com.haruhi.botserver.features.reply.handler;

import com.haruhi.botserver.bot.handler.INoticeHandler;

import com.haruhi.botserver.bot.session.Bot;
import com.haruhi.botserver.infrastructure.concurrent.ThreadPoolUtil;
import com.haruhi.botserver.integration.onebot.model.MessageHolder;
import com.haruhi.botserver.integration.onebot.model.NoticeTypeEnum;
import com.haruhi.botserver.integration.onebot.model.SubTypeEnum;
import com.haruhi.botserver.integration.onebot.model.event.NoticeEvent;
import com.haruhi.botserver.shared.util.CommonUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 处理戳一戳
 */
@Slf4j
@Component
public class PokeMeHandler implements INoticeHandler {

    public volatile static List<String> cache = new CopyOnWriteArrayList<>();

    @Override
    public boolean support(NoticeEvent event) {
        return NoticeTypeEnum.notify.name().equals(event.getNoticeType())
                && SubTypeEnum.poke.name().equals(event.getSubType());
    }

    @Override
    public void onNotice(final Bot bot, final NoticeEvent event) {
        if (cache.isEmpty()) {
            return;
        }
        // targetId: 被戳人
        if(!Objects.equals(event.getSelfId(), event.getTargetId())
                || Objects.equals(event.getSelfId(), event.getUserId())){
            // 只对戳了机器人生效
            return;
        }

        ThreadPoolUtil.getHandleCommandPool().execute(()->{
            try {
                int size = cache.size();
                if(size > 0){
                    String reply = cache.get(CommonUtil.randomInt(0, size - 1));
                    if(StringUtils.isEmpty(reply)){
                        bot.sendPoke(event.getUserId(), event.getGroupId());
                    }else{
                        bot.sendMessage(event.getUserId(),event.getGroupId(),event.getMessageType(), MessageHolder.instanceText(reply));
                    }
                }
            }catch (Exception e){
                log.error("处理戳一戳发生异常",e);
            }
        });
    }
}
