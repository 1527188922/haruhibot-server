package com.haruhi.botserver.administration.handler;

import com.haruhi.botserver.bot.handler.IAllMessageHandler;


import com.haruhi.botserver.shared.annotation.SuperuserAuthentication;
import com.haruhi.botserver.bot.handler.HandlerWeightEnum;
import com.haruhi.botserver.shared.constant.RegexEnum;
import com.haruhi.botserver.integration.onebot.model.Message;
import com.haruhi.botserver.administration.service.SystemService;
import com.haruhi.botserver.infrastructure.concurrent.ThreadPoolUtil;
import com.haruhi.botserver.bot.session.Bot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
public class RefreshCacheHandler implements IAllMessageHandler {
    
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
                systemService.loadCache(3);
                bot.sendMessage(message.getUserId(), message.getGroupId(), message.getMessageType(),
                        "刷新缓存完成\n耗时：" + (System.currentTimeMillis() - l) + "ms", true);
            }finally {
                REFRESH_LOCK.set(false);
            }
        });
        return true;
    }
}
