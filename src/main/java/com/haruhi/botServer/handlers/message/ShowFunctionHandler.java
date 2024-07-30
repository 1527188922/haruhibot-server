package com.haruhi.botServer.handlers.message;

import com.haruhi.botServer.constant.RegexEnum;
import com.haruhi.botServer.dispenser.MessageDispenser;
import com.haruhi.botServer.dto.gocq.response.Message;
import com.haruhi.botServer.event.message.IAllMessageEvent;
import com.haruhi.botServer.event.message.IMessageEvent;
import com.haruhi.botServer.utils.ThreadPoolUtil;
import com.haruhi.botServer.ws.Server;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Collection;
import java.util.Comparator;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ShowFunctionHandler implements IAllMessageEvent {

    @Override
    public int weight() {
        return 104;
    }

    @Override
    public String funName() {
        return "显示所有功能";
    }
    @Override
    public boolean onMessage(final WebSocketSession session,final Message message) {
        if (!message.getRawMessage().matches(RegexEnum.SHOW_ALL_FUNCTION.getValue())) {
            return false;
        }
        ThreadPoolUtil.getHandleCommandPool().execute(new Task(session,message));
        return true;
    }

    private class Task implements Runnable{
        private WebSocketSession session;
        private Message message;
        Task(WebSocketSession session,Message message){
            this.session = session;
            this.message = message;
        }
        @Override
        public void run() {
            Collection<IMessageEvent> values = MessageDispenser.getMessageEventMap().values().stream().sorted(Comparator.comparing(IMessageEvent::weight)).collect(Collectors.toList());
            StringBuilder stringBuilder = new StringBuilder("所有功能：\n");
            for (IMessageEvent eventType : values) {
                stringBuilder.append("id：").append(eventType.weight()).append("\n");
                stringBuilder.append("名称：").append(eventType.funName()).append("\n");
            }
            Server.sendMessage(session,message.getUserId(),message.getGroupId(),message.getMessageType(),stringBuilder.toString(),true);
        }
    }

}
