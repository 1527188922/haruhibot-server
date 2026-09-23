package com.haruhi.botserver.features.chatrecord.handler;

import com.alibaba.fastjson.JSONObject;
import com.haruhi.botserver.bot.handler.HandlerWeightEnum;
import com.haruhi.botserver.integration.onebot.model.Message;
import com.haruhi.botserver.bot.handler.IAllMessageHandler;
import com.haruhi.botserver.features.chatrecord.service.ChatRecordService;
import com.haruhi.botserver.infrastructure.concurrent.ThreadPoolUtil;
import com.haruhi.botserver.bot.session.Bot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Slf4j
@Component
public class ChatRecordHandler implements IAllMessageHandler {
    @Override
    public boolean bypassGroupRestrictions() {
        return true;
    }

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
