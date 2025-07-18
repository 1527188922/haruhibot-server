package com.haruhi.botServer.handlers.message.chatRecord;

import com.haruhi.botServer.constant.HandlerWeightEnum;
import com.haruhi.botServer.constant.TimeUnitEnum;
import com.haruhi.botServer.dto.qqclient.Message;
import com.haruhi.botServer.event.message.IGroupMessageEvent;
import com.haruhi.botServer.service.ChatRecordSqliteService;
import com.haruhi.botServer.utils.ThreadPoolUtil;
import com.haruhi.botServer.ws.Bot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Slf4j
@Component
public class GroupWordCloudHandler implements IGroupMessageEvent {

    @Override
    public int weight() {
        return HandlerWeightEnum.W_540.getWeight();
    }

    @Override
    public String funName() {
        return HandlerWeightEnum.W_540.getName();
    }

    public static final Map<String, Integer> lock = new ConcurrentHashMap<>();

    @Autowired
    private ChatRecordSqliteService chatRecordSqliteService;

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
    public boolean onGroup(Bot bot, final Message message) {
        RegexEnum matching = matching(message.getRawMessage());
        if (matching == null) {
            return false;
        }
        if(lock.containsKey(String.valueOf(message.getGroupId()) + message.getSelfId())){
            bot.sendGroupMessage(message.getGroupId(),"词云正在生成中...莫着急",true);
            return true;
        }else{
            lock.put(String.valueOf(message.getGroupId()) + message.getSelfId(),1);
        }
        ThreadPoolUtil.getHandleCommandPool().execute(()->{
            chatRecordSqliteService.sendWordCloudImage(bot,matching, message);
        });
        return true;
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
