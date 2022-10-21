package com.haruhi.botServer.handlers.message;

import com.haruhi.botServer.constant.RegexEnum;
import com.haruhi.botServer.constant.event.MessageEventEnum;
import com.haruhi.botServer.dispenser.MessageDispenser;
import com.haruhi.botServer.dto.gocq.response.Message;
import com.haruhi.botServer.event.message.IMessageEvent;
import com.haruhi.botServer.event.message.IMessageEventType;
import com.haruhi.botServer.factory.ThreadPoolFactory;
import com.haruhi.botServer.ws.Server;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Collection;
import java.util.Comparator;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ShowFunctionHandler implements IMessageEvent {

    @Override
    public int weight() {
        return 104;
    }

    @Override
    public String funName() {
        return "显示所有功能";
    }
    @Override
    public boolean onMessage(final WebSocketSession session,final Message message, final String command) {
        if (!command.matches(RegexEnum.SHOW_ALL_FUNCTION.getValue())) {
            return false;
        }
        ThreadPoolFactory.getCommandHandlerThreadPool().execute(new Task(session,message));
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
            Collection<IMessageEventType> values = MessageDispenser.getMessageEventTypeMap().values().stream().sorted(Comparator.comparing(IMessageEventType::weight)).collect(Collectors.toList());
            StringBuilder stringBuilder = new StringBuilder("所有功能：\n");
            for (IMessageEventType eventType : values) {
                stringBuilder.append("id：").append(eventType.weight()).append("\n");
                stringBuilder.append("名称：").append(eventType.funName()).append("\n");
            }
            Server.sendMessage(session,message.getUser_id(),message.getGroup_id(),message.getMessage_type(),stringBuilder.toString(),true);
        }
    }

}
