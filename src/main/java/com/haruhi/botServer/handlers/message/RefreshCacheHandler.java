package com.haruhi.botServer.handlers.message;

import com.haruhi.botServer.annotation.SuperuserAuthentication;
import com.haruhi.botServer.constant.HandlerWeightEnum;
import com.haruhi.botServer.constant.RegexEnum;
import com.haruhi.botServer.dto.qqclient.Message;
import com.haruhi.botServer.event.message.IAllMessageEvent;
import com.haruhi.botServer.service.SystemService;
import com.haruhi.botServer.utils.ThreadPoolUtil;
import com.haruhi.botServer.ws.Bot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
public class RefreshCacheHandler implements IAllMessageEvent {
    
    @Autowired
    private SystemService systemService;

    @Override
    public int weight() {
        return HandlerWeightEnum.W_200.getWeight();
    }

    @Override
    public String funName() {
        return HandlerWeightEnum.W_200.getName();
    }
    
    private static final AtomicBoolean REFRESH_LOCK = new AtomicBoolean(false);

    @Override
    @SuperuserAuthentication
    public boolean onMessage(Bot bot, Message message) {
        String cmd;
        if(message.isAtBot()){
            cmd = message.getText(-1);
        }else{
            cmd = message.getRawMessage();
        }
        if (!cmd.trim().matches(RegexEnum.FLUSH_CACHE.getValue())){
            return false;
        }

        if(!REFRESH_LOCK.compareAndSet(false,true)){
            bot.sendMessage(message.getUserId(), message.getGroupId(), message.getMessageType(),
                    "正在刷新中...", true);
            return true;
        }
        
        ThreadPoolUtil.getHandleCommandPool().execute(()->{
            try {
                long l = System.currentTimeMillis();
                systemService.clearCache();
                systemService.loadCache();
                bot.sendMessage(message.getUserId(), message.getGroupId(), message.getMessageType(),
                        "刷新缓存完成\n耗时：" + (System.currentTimeMillis() - l) + "ms", true);
            }finally {
                REFRESH_LOCK.set(false);
            }
        });
        return true;
    }
}
