package com.haruhi.botServer.handlers.message.chatRecord;

import com.alibaba.fastjson.JSONObject;
import com.haruhi.botServer.constant.HandlerWeightEnum;
import com.haruhi.botServer.dto.gocq.response.Message;
import com.haruhi.botServer.entity.ChatRecord;
import com.haruhi.botServer.event.message.IAllMessageEvent;
import com.haruhi.botServer.service.chatRecord.ChatRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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

    @Autowired
    private ChatRecordService chatRecordService;

    private static ExecutorService threadPool;
    public SavaChatRecordHandler(){
        if (threadPool == null) {
            threadPool = new ThreadPoolExecutor(1, 1,15L * 60L, TimeUnit.SECONDS,
                    new LinkedBlockingQueue<Runnable>(),new CustomizableThreadFactory("pool-insertRecord-"));
        }
    }

    /**
     * 聊天记录入库
     * 不参与命令处理,最终返回false
     * @param message
     * @return
     */
    @Override
    public boolean onMessage(WebSocketSession session, Message message) {
        threadPool.execute(new Task(chatRecordService, message));
        return false;
    }

    private static class Task implements Runnable{
        private final ChatRecordService service;
        private final Message message;
        public Task(ChatRecordService service, final Message message){
            this.service = service;
            this.message = message;
        }

        @Override
        public void run() {
            ChatRecord param = new ChatRecord();
            try {
                if(message.getSender() != null){
                    param.setCard(message.getSender().getCard());
                    param.setNickname(message.getSender().getNickname());
                }
                param.setGroupId(message.getGroupId());
                param.setUserId(message.getUserId());
                param.setContent(message.getRawMessage());
                param.setSelfId(message.getSelfId());
                param.setMessageId(message.getMessageId());
                param.setMessageType(message.getMessageType());
                if(message.getTime() != null && String.valueOf(message.getTime()).length() == 10){
                    param.setCreateTime(message.getTime() * 1000);
                }else{
                    param.setCreateTime(message.getTime());
                }
                service.save(param);
            }catch (Exception e){
                log.error("保存聊天记录异常 {}", JSONObject.toJSONString(param),e);
            }
        }
    }
}
