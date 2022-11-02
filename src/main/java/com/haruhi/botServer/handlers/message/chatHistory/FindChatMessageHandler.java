package com.haruhi.botServer.handlers.message.chatHistory;

import com.haruhi.botServer.constant.TimeUnitEnum;
import com.haruhi.botServer.dto.gocq.response.Message;
import com.haruhi.botServer.event.message.IGroupMessageEvent;
import com.haruhi.botServer.utils.ThreadPoolUtil;
import com.haruhi.botServer.service.groupChatHistory.GroupChatHistoryService;
import com.haruhi.botServer.ws.Server;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.text.MessageFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class FindChatMessageHandler implements IGroupMessageEvent {
    @Override
    public int weight() {
        return 91;
    }

    @Override
    public String funName() {
        return "群聊天记录搜索";
    }

    @Autowired
    private GroupChatHistoryService groupChatHistoryService;

    @Override
    public boolean onGroup(final WebSocketSession session,final Message message, final String command) {
        Param param = matching(session,message,command);
        if(param == null){
            return false;
        }

        ThreadPoolUtil.getHandleCommandPool().execute(new Task(session,groupChatHistoryService,message,param));
        return true;
    }

    private class Task implements Runnable{
        private WebSocketSession session;
        private GroupChatHistoryService service;
        private Message message;
        private Param param;

        public Task(WebSocketSession session,GroupChatHistoryService service, Message message, Param param){
            this.session = session;
            this.service = service;
            this.message = message;
            this.param = param;
        }
        @Override
        public void run() {
            service.sendChatList(session,message,param);
        }
    }

    private Param matching(final WebSocketSession session,final Message message,final String command){
        for (Regex item : Regex.values()) {
            Pattern compile = Pattern.compile(item.value);
            Matcher matcher = compile.matcher(command);
            if(matcher.find()){
                Integer num = null;
                String args = null;
                try {
                    args = matcher.group(1);
                    num = Integer.valueOf(args);
                }catch (Exception e){
                    Server.sendGroupMessage(session,message.getGroup_id(),MessageFormat.format("错误的参数[{0}],请输入整数...",args),true);
                    return null;
                }
                return new Param(num,item.timeUnit,item.messageType);
            }
        }
        return null;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Param {
        private Integer num;
        private TimeUnitEnum unit;
        private MessageType messageType;
    }

    public enum Regex{

        DAY_ALL("聊天记录(.*?)天",TimeUnitEnum.DAY, MessageType.ALL),
        HOUR_ALL("聊天记录(.*?)时",TimeUnitEnum.HOUR, MessageType.ALL),
        DAY_IMAGE("聊天图片(.*?)天",TimeUnitEnum.DAY, MessageType.IMAGE),
        HOUR_IMAGE("聊天图片(.*?)时",TimeUnitEnum.HOUR, MessageType.IMAGE),
        DAY_TXT("聊天文字(.*?)天",TimeUnitEnum.DAY, MessageType.TXT),
        HOUR_TXT("聊天文字(.*?)时",TimeUnitEnum.HOUR, MessageType.TXT);

        private String value;
        private TimeUnitEnum timeUnit;
        private MessageType messageType;
        Regex(String value,TimeUnitEnum timeUnit, MessageType messageType){
            this.value = value;
            this.timeUnit = timeUnit;
            this.messageType = messageType;
        }
    }

    public enum MessageType{
        ALL,IMAGE,TXT
    }
}
