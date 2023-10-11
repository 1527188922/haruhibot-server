package com.haruhi.botServer.handlers.message;

import com.haruhi.botServer.annotation.SuperuserAuthentication;
import com.haruhi.botServer.constant.RegexEnum;
import com.haruhi.botServer.dto.gocq.response.Message;
import com.haruhi.botServer.event.message.IAllMessageEvent;
import com.haruhi.botServer.service.SystemService;
import com.haruhi.botServer.utils.CommonUtil;
import com.haruhi.botServer.utils.ThreadPoolUtil;
import com.haruhi.botServer.ws.Server;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

@Slf4j
@Component
public class RefreshCacheHandler implements IAllMessageEvent {
    
    @Autowired
    private SystemService systemService;

    @Override
    public int weight() {
        return 5;
    }

    @Override
    public String funName() {
        return "刷新缓存";
    }

    @Override
    @SuperuserAuthentication
    public boolean onMessage(WebSocketSession session, Message message, String command) {
        String cmd;
        if(CommonUtil.isAt(message.getSelfId(),command)){
            cmd = command.replaceAll(RegexEnum.CQ_CODE_REPLACR.getValue(),"");
        }else{
            cmd = command;
        }
        if (!cmd.trim().matches(RegexEnum.FLUSH_CACHE.getValue())){
            return false;
        }
        ThreadPoolUtil.getHandleCommandPool().execute(()->{
            long l = System.currentTimeMillis();
            systemService.clearCache();
            systemService.loadCache();
            Server.sendMessage(session,message.getUserId(), message.getGroupId(), message.getMessageType(), 
                    "刷新缓存完成\n耗时：" + (System.currentTimeMillis() - l) + "ms", true);
        });
        
        return true;
    }
}
