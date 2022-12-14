package com.haruhi.botServer.handlers.notice;

import com.haruhi.botServer.dto.gocq.response.Message;
import com.haruhi.botServer.event.notice.IGroupDecreaseEvent;
import com.haruhi.botServer.utils.ThreadPoolUtil;
import com.haruhi.botServer.ws.Server;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.text.MessageFormat;

@Slf4j
@Component
public class GroupDecreaseHandler implements IGroupDecreaseEvent {


    @Override
    public void onGroupDecrease(final WebSocketSession session,final Message message) {
        ThreadPoolUtil.getHandleCommandPool().execute(()->{
            Server.sendGroupMessage(session, message.getGroup_id(), MessageFormat.format("{0} 离开了本群。",message.getUser_id()), true);
        });
    }
}
