package com.haruhi.botServer.handlers.message.chatRecord;

import com.alibaba.fastjson.JSONObject;
import com.haruhi.botServer.constant.HandlerWeightEnum;
import com.haruhi.botServer.dto.gocq.response.Message;
import com.haruhi.botServer.entity.ChatRecord;
import com.haruhi.botServer.event.message.IAllMessageEvent;
import com.haruhi.botServer.service.chatRecord.ChatRecordService;
import com.haruhi.botServer.utils.ThreadPoolUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

@Slf4j
@Component
public class SavaChatRecordHandler implements IAllMessageEvent {
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
    private ChatRecordService chatRecordService;

    /**
     * 聊天记录入库
     * 不参与命令处理,最终返回false
     * @param message
     * @return
     */
    @Override
    public boolean onMessage(WebSocketSession session, Message message) {

        ThreadPoolUtil.getSharePool().execute(()->{

            ChatRecord record = new ChatRecord();
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
                if(message.getTime() != null && String.valueOf(message.getTime()).length() == 10){
                    record.setCreateTime(message.getTime() * 1000);
                }else{
                    record.setCreateTime(message.getTime());
                }
                chatRecordService.save(record);
            }catch (Exception e){
                log.error("保存聊天记录异常 {}", JSONObject.toJSONString(record),e);
            }
        });

        return false;
    }
}
