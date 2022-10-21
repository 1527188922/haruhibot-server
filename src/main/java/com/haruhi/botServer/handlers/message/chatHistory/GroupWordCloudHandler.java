package com.haruhi.botServer.handlers.message.chatHistory;

import com.haruhi.botServer.constant.TimeUnitEnum;
import com.haruhi.botServer.dto.gocq.response.Message;
import com.haruhi.botServer.event.message.IGroupMessageEvent;
import com.haruhi.botServer.factory.ThreadPoolFactory;
import com.haruhi.botServer.service.groupChatHistory.GroupChatHistoryService;
import com.haruhi.botServer.ws.Server;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Slf4j
@Component
public class GroupWordCloudHandler implements IGroupMessageEvent {

    @Override
    public int weight() {
        return 90;
    }

    @Override
    public String funName() {
        return "群词云";
    }

    public static volatile Map<String, Integer> lock = new ConcurrentHashMap<>(4);

    @Autowired
    private GroupChatHistoryService groupChatHistoryService;

    private RegexEnum matching(final String command){
        String s = command.replaceAll(com.haruhi.botServer.constant.RegexEnum.CQ_CODE_REPLACR.getValue(), "").trim();
        if(s.matches(RegexEnum.YEAR.regex)){
            return RegexEnum.YEAR;
        }else if (s.matches(RegexEnum.MONTH.regex)){
            return RegexEnum.MONTH;
        }else if(s.matches(RegexEnum.WEEK.regex)){
            return RegexEnum.WEEK;
        }else if(s.matches(RegexEnum.DAY.regex)){
            return RegexEnum.DAY;
        }
        return null;
    }
    @Override
    public boolean onGroup(final WebSocketSession session,final Message message, final String command) {
        RegexEnum matching = matching(command);
        if (matching == null) {
            return false;
        }
        if(lock.containsKey(String.valueOf(message.getGroup_id()))){
            Server.sendGroupMessage(session,message.getGroup_id(),"词云正在生成中...莫着急",true);
            return true;
        }else{
            lock.put(String.valueOf(message.getGroup_id()),1);
        }
        ThreadPoolFactory.getCommandHandlerThreadPool().execute(new Task(session,groupChatHistoryService,matching,message));
        return true;
    }
    private class Task implements Runnable{
        private WebSocketSession session;
        private GroupChatHistoryService groupChatHistoryService;
        private RegexEnum regexEnum;
        private Message message;
        public Task(WebSocketSession session, GroupChatHistoryService groupChatHistoryService,RegexEnum regexEnum,Message message){
            this.session = session;
            this.groupChatHistoryService = groupChatHistoryService;
            this.regexEnum = regexEnum;
            this.message = message;
        }
        @Override
        public void run() {
            groupChatHistoryService.sendWordCloudImage(session,regexEnum, message);
        }
    }
    public enum RegexEnum{
        YEAR("年度词云",TimeUnitEnum.YEAR),
        MONTH("本月词云",TimeUnitEnum.MONTH),
        WEEK("本周词云",TimeUnitEnum.WEEK),
        DAY("今日词云",TimeUnitEnum.DAY);

        private String regex;
        private TimeUnitEnum unit;
        RegexEnum(String regex, TimeUnitEnum unit){
            this.regex = regex;
            this.unit = unit;
        }
        public TimeUnitEnum getUnit(){
            return unit;
        }
        public String getRegex(){
            return regex;
        }
    }

}
