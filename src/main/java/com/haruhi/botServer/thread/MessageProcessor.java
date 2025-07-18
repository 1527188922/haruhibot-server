package com.haruhi.botServer.thread;

import com.haruhi.botServer.constant.event.MetaEventEnum;
import com.haruhi.botServer.constant.event.PostTypeEnum;
import com.haruhi.botServer.constant.event.SubTypeEnum;
import com.haruhi.botServer.dispenser.MessageDispenser;
import com.haruhi.botServer.dispenser.NoticeDispenser;
import com.haruhi.botServer.dto.qqclient.Message;
import com.haruhi.botServer.thread.pool.policy.ShareRunsPolicy;
import com.haruhi.botServer.ws.Bot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class MessageProcessor{

    private final static ThreadPoolExecutor threadPool;

    @Autowired
    private MessageDispenser messageDispenser;
    @Autowired
    private NoticeDispenser noticeDispenser;
    static {
        threadPool = new ThreadPoolExecutor(6, 10, 10L * 60L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(100), new CustomizableThreadFactory("pool-processMessage-"), new ShareRunsPolicy("pool-processMessage"));
    }

    public void execute(Bot bot, Message message){

        threadPool.execute(()->{
            try {
                if(PostTypeEnum.message.name().equals(message.getPostType())
                        || PostTypeEnum.message_sent.name().equals(message.getPostType())){
                    // 普通消息
                    if(message.getRawMessage() != null){
                        messageDispenser.onEvent(bot, message);
                    }
                }else if(PostTypeEnum.notice.name().equals(message.getPostType())){
                    // bot通知
                    noticeDispenser.onEvent(bot, message);
                } else if(PostTypeEnum.meta_event.name().equals(message.getPostType())){
                    // 系统消息
                    handleMetaEvent(bot,message);
                }else {
                    log.info("未知PostType: {}", message.getPostType());
                }
            }catch (Exception e){
                log.error("处理消息时异常",e);
            }
        });
    }


    private void handleMetaEvent(Bot bot, Message message){
        if(MetaEventEnum.heartbeat.toString().equals(message.getMetaEventType())){
            // 心跳包
            return;
        }
        if(MetaEventEnum.lifecycle.toString().equals(message.getMetaEventType())
                && SubTypeEnum.connect.toString().equals(message.getSubType())){
            // 刚连接成功时，gocq会发一条消息给bot
            bot.setId(message.getSelfId());
            log.info("收到QQ号连接：{} sessionId：{}",message.getSelfId(),bot.getSessionId());
        }
    }
}
