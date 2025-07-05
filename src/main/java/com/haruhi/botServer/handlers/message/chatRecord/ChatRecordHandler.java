package com.haruhi.botServer.handlers.message.chatRecord;

import com.alibaba.fastjson.JSONObject;
import com.haruhi.botServer.constant.HandlerWeightEnum;
import com.haruhi.botServer.dto.gocq.response.Message;
import com.haruhi.botServer.entity.sqlite.ChatRecordSqlite;
import com.haruhi.botServer.event.message.IAllMessageEvent;
import com.haruhi.botServer.service.ChatRecordSqliteService;
import com.haruhi.botServer.utils.DateTimeUtil;
import com.haruhi.botServer.utils.ThreadPoolUtil;
import com.haruhi.botServer.ws.Bot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;

@Slf4j
@Component
public class ChatRecordHandler implements IAllMessageEvent {
    @Override
    public int weight() {
        return HandlerWeightEnum.W_999.getWeight();
    }

    @Override
    public String funName() {
        return HandlerWeightEnum.W_999.getName();
    }

    @Override
    public boolean handleSelfMsg() {
        return true;
    }
    @Autowired
    private ChatRecordSqliteService ChatRecordSqliteService;


    /**
     * 聊天记录入库
     * 不参与命令处理,最终返回false
     * @param message
     * @return
     */
    @Override
    public boolean onMessage(Bot bot, Message message) {

        ThreadPoolUtil.getHandleCommandPool().execute(()->{

            ChatRecordSqlite record = new ChatRecordSqlite();
            try {
                if(message.getSender() != null){
                    record.setCard(message.getSender().getCard());
                    record.setNickname(message.getSender().getNickname());
                }
                record.setGroupId(message.getGroupId());
                record.setUserId(message.getUserId());
                record.setContent(message.getRawMessage());
                record.setSelfId(message.getSelfId());
                record.setMessageId(message.getMessageId());
                record.setMessageType(message.getMessageType());
                setTime(message, record);
                ChatRecordSqliteService.save(record);
            }catch (Exception e){
                log.error("保存聊天记录异常 {}", JSONObject.toJSONString(record),e);
            }
        },false);

        return false;
    }

    private void setTime(Message message, ChatRecordSqlite record){
        if (message.getTime() == null) {
            record.setTime(DateTimeUtil.dateTimeFormat(new Date(), DateTimeUtil.PatternEnum.yyyyMMddHHmmss));
            return;
        }
        record.setTime(DateTimeUtil.dateTimeFormat(message.getTime(), DateTimeUtil.PatternEnum.yyyyMMddHHmmss));
    }
}
