package com.haruhi.botServer.handlers.notice;

import com.haruhi.botServer.dto.qqclient.Message;
import com.haruhi.botServer.dto.qqclient.MessageHolder;
import com.haruhi.botServer.event.notice.IPokeEvent;
import com.haruhi.botServer.utils.ThreadPoolUtil;
import com.haruhi.botServer.utils.CommonUtil;
import com.haruhi.botServer.ws.Bot;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 处理戳一戳
 */
@Slf4j
@Component
public class PokeMeHandler implements IPokeEvent {

    public volatile static List<String> cache = new CopyOnWriteArrayList<>();

    @Override
    public void onPoke(final Bot bot, final Message message) {
        if (cache.isEmpty()) {
            return;
        }
        if(!String.valueOf(message.getSelfId()).equals(String.valueOf(message.getTargetId()))
                || String.valueOf(message.getSelfId()).equals(String.valueOf(message.getUserId()))){
            // 只对戳了机器人生效
            return;
        }

        ThreadPoolUtil.getHandleCommandPool().execute(()->{
            try {
                int size = cache.size();
                if(size > 0){
                    String reply = cache.get(CommonUtil.randomInt(0, size - 1));
                    if(StringUtils.isEmpty(reply)){
                        bot.sendPoke(message.getUserId(), message.getGroupId());
                    }else{
                        bot.sendMessage(message.getUserId(),message.getGroupId(),message.getMessageType(), MessageHolder.instanceText(reply));
                    }
                }
            }catch (Exception e){
                log.error("处理戳一戳发生异常",e);
            }
        });
    }
}
