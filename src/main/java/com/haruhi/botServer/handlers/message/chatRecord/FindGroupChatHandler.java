package com.haruhi.botServer.handlers.message.chatRecord;

import com.haruhi.botServer.constant.HandlerWeightEnum;
import com.haruhi.botServer.constant.TimeUnitEnum;
import com.haruhi.botServer.dto.gocq.response.Message;
import com.haruhi.botServer.event.message.IGroupMessageEvent;
import com.haruhi.botServer.utils.ThreadPoolUtil;
import com.haruhi.botServer.service.chatRecord.ChatRecordService;
import com.haruhi.botServer.ws.Bot;
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
public class FindGroupChatHandler implements IGroupMessageEvent {
    @Override
    public int weight() {
        return HandlerWeightEnum.W_600.getWeight();
    }

    @Override
    public String funName() {
        return HandlerWeightEnum.W_600.getName();
    }

    @Autowired
    private ChatRecordService chatRecordService;

    @Override
    public boolean onGroup(Bot bot, final Message message) {
        Param param = matching(bot,message);
        if(param == null){
            return false;
        }

        ThreadPoolUtil.getHandleCommandPool().execute(()->{
            chatRecordService.sendGroupChatList(bot,message,param);
        });
        return true;
    }


    private Param matching(Bot bot,final Message message){
        for (Regex item : Regex.values()) {
            if(!message.getRawMessage().startsWith(item.prefix)){
                continue;
            }
            Pattern compile = Pattern.compile(item.getValue());
            Matcher matcher = compile.matcher(message.getRawMessage());
            if(matcher.find()){
                Integer num = null;
                String args = null;
                try {
                    args = matcher.group(1);
                    num = Integer.valueOf(args);
                }catch (Exception e){
                    bot.sendGroupMessage(message.getGroupId(),MessageFormat.format("错误的参数[{0}],请输入整数...",args),true);
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

    @AllArgsConstructor
    public enum Regex{

        DAY_ALL("(.*?)天","聊天记录",TimeUnitEnum.DAY, MessageType.ALL),
        HOUR_ALL("(.*?)时","聊天记录",TimeUnitEnum.HOUR, MessageType.ALL),
        DAY_IMAGE("(.*?)天","聊天图片",TimeUnitEnum.DAY, MessageType.IMAGE),
        HOUR_IMAGE("(.*?)时","聊天图片",TimeUnitEnum.HOUR, MessageType.IMAGE),
        DAY_TXT("(.*?)天","聊天文字",TimeUnitEnum.DAY, MessageType.TXT),
        HOUR_TXT("(.*?)时","聊天文字",TimeUnitEnum.HOUR, MessageType.TXT);

        private String value;
        private String prefix;
        private TimeUnitEnum timeUnit;
        private MessageType messageType;

        public String getValue(){
            return prefix + value;
        }

    }

    public enum MessageType{
        ALL,IMAGE,TXT
    }
}
