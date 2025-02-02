package com.haruhi.botServer.handlers.notice;

import com.haruhi.botServer.constant.CqCodeTypeEnum;
import com.haruhi.botServer.dto.gocq.response.Message;
import com.haruhi.botServer.event.notice.IPokeEvent;
import com.haruhi.botServer.utils.ThreadPoolUtil;
import com.haruhi.botServer.utils.CommonUtil;
import com.haruhi.botServer.ws.Bot;
import com.simplerobot.modules.utils.KQCodeUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
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
        if(!String.valueOf(message.getSelfId()).equals(String.valueOf(message.getTargetId())) || String.valueOf(message.getSelfId()).equals(String.valueOf(message.getUserId())) || cache.size() == 0){
            // 只对戳了机器人生效
            return;
        }

        ThreadPoolUtil.getHandleCommandPool().execute(()->{
            try {
                int size = cache.size();
                if(size > 0){
                    String reply = cache.get(CommonUtil.randomInt(0, size - 1));
                    if(message.isGroupMsg()){
                        if("".equals(reply)){
                            KQCodeUtils instance = KQCodeUtils.getInstance();
                            String s = instance.toCq(CqCodeTypeEnum.poke.getType(), "qq=" + message.getUserId());
                            bot.sendGroupMessage(message.getGroupId(), s, false);
                        }else{
                            bot.sendGroupMessage(message.getGroupId(),reply, true);
                        }
                    }else if(message.isPrivateMsg()){
                        // gocq私聊不能发送给戳一戳 所以这里只回复文字
                        while (Strings.isBlank(reply)){
                            reply = cache.get(CommonUtil.randomInt(0, size - 1));
                        }
                        bot.sendPrivateMessage(message.getUserId(),reply, true);
                    }
                }
            }catch (Exception e){
                log.error("处理戳一戳发生异常",e);
            }
        });
    }
}
