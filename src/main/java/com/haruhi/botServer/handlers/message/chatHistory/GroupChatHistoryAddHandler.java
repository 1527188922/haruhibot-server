package com.haruhi.botServer.handlers.message.chatHistory;

import com.haruhi.botServer.dto.gocq.response.Message;
import com.haruhi.botServer.entity.GroupChatHistory;
import com.haruhi.botServer.event.message.IGroupMessageEvent;
import com.haruhi.botServer.factory.ThreadPoolFactory;
import com.haruhi.botServer.service.groupChatHistory.GroupChatHistoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

@Slf4j
@Component
public class GroupChatHistoryAddHandler implements IGroupMessageEvent {
    @Override
    public int weight() {
        return 997;
    }

    @Override
    public String funName() {
        return "群聊记录入库";
    }

    @Autowired
    private GroupChatHistoryService groupChatHistoryService;

    /**
     * 群聊历史聊天入库
     * 不参与命令处理,最终返回false
     * @param message
     * @param command
     * @return
     */
    @Override
    public boolean onGroup(final WebSocketSession session,final Message message, final String command) {
        ThreadPoolFactory.getChatHistoryThreadPool().execute(new Task(groupChatHistoryService,message));
        return false;
    }

    private class Task implements Runnable{
        private GroupChatHistoryService service;
        private Message message;
        public Task(GroupChatHistoryService service,final Message message){
            this.service = service;
            this.message = message;
        }

        @Override
        public void run() {
            try {
                GroupChatHistory param = new GroupChatHistory();
                param.setId(null);
                param.setCard(message.getSender().getCard());
                param.setNickname(message.getSender().getNickname());
                param.setGroupId(message.getGroup_id());
                param.setUserId(message.getUser_id());
                param.setContent(message.getMessage());
                param.setSelfId(message.getSelf_id());
                param.setCreateTime(message.getTime() * 1000);
                param.setMessageId(message.getMessage_id());
                service.save(param);
            }catch (Exception e){
                log.error("群聊天历史入库异常",e);
            }
        }
    }
}
