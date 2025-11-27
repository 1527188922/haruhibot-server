package com.haruhi.botServer.handlers.message.chatRecord;

import com.alibaba.fastjson.JSONObject;
import com.haruhi.botServer.constant.HandlerWeightEnum;
import com.haruhi.botServer.dto.qqclient.Message;
import com.haruhi.botServer.entity.ChatRecordExtendSqlite;
import com.haruhi.botServer.entity.ChatRecordSqlite;
import com.haruhi.botServer.event.message.IAllMessageEvent;
import com.haruhi.botServer.mapper.ChatRecordExtendSqliteMapper;
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
    private ChatRecordSqliteService chatRecordSqliteService;
    @Autowired
    private ChatRecordExtendSqliteMapper chatRecordExtendSqliteMapper;


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
                if (chatRecordSqliteService.save(record)) {
                    ChatRecordExtendSqlite chatRecordExtendSqlite = new ChatRecordExtendSqlite();
                    chatRecordExtendSqlite.setChatRecordId(record.getId());
                    chatRecordExtendSqlite.setMessageId(record.getMessageId());
                    chatRecordExtendSqlite.setRawWsMessage(message.getRawWsMsg());
                    chatRecordExtendSqlite.setTime(record.getTime());
                    chatRecordExtendSqliteMapper.insert(chatRecordExtendSqlite);
                }
            }catch (Exception e){
                log.error("保存聊天记录异常 {}", JSONObject.toJSONString(record),e);
            }
        });

        return false;
    }

    private void setTime(Message message, ChatRecordSqlite record){
        if (message.getTime() == null) {
            record.setTime(DateTimeUtil.dateTimeFormat(new Date(), DateTimeUtil.PatternEnum.yyyyMMddHHmmss));
            return;
        }

        if(String.valueOf(message.getTime()).length() == 10){
            record.setTime(DateTimeUtil.dateTimeFormat(new Date(message.getTime() * 1000), DateTimeUtil.PatternEnum.yyyyMMddHHmmss));
        }else{
            record.setTime(DateTimeUtil.dateTimeFormat(message.getTime(), DateTimeUtil.PatternEnum.yyyyMMddHHmmss));
        }
    }
}
