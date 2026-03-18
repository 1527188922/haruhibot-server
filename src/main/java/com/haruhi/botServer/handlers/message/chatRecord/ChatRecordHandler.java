package com.haruhi.botServer.handlers.message.chatRecord;

import com.alibaba.fastjson.JSONObject;
import com.haruhi.botServer.constant.HandlerWeightEnum;
import com.haruhi.botServer.dto.qqclient.Message;
import com.haruhi.botServer.event.message.IAllMessageEvent;
import com.haruhi.botServer.service.ChatRecordService;
import com.haruhi.botServer.utils.ThreadPoolUtil;
import com.haruhi.botServer.ws.Bot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


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
    private ChatRecordService chatRecordService;


    /**
     * 聊天记录入库
     * 不参与命令处理,最终返回false
     * @param message
     * @return
     */
    @Override
    public boolean onMessage(Bot bot, Message message) {

        ThreadPoolUtil.getHandleCommandPool().execute(()->{
            try {
                chatRecordService.saveChatRecord(message);
            }catch (Exception e){
                log.error("保存聊天记录异常 {}", JSONObject.toJSONString(message),e);
            }
        });

        return false;
    }
}
