package com.haruhi.botServer.handlers.message.system;

import com.haruhi.botServer.annotation.SuperuserAuthentication;
import com.haruhi.botServer.dto.gocq.response.Message;
import com.haruhi.botServer.event.message.IAllMessageEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

@Component
@Slf4j
public class StatusHandler implements IAllMessageEvent {


    @Override
    public int weight() {
        return 60;
    }

    @Override
    public String funName() {
        return "查看状态";
    }


    @SuperuserAuthentication
    @Override
    public boolean onMessage(final WebSocketSession session,final Message message,final String command) {

        if ("/状态".equals(command)) {
            log.info("执行了:{}",funName());
            return true;
        }
        return false;
    }
}
