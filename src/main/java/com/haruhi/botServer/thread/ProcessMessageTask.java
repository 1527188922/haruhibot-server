package com.haruhi.botServer.thread;

import com.haruhi.botServer.constant.event.MetaEventEnum;
import com.haruhi.botServer.constant.event.PostTypeEnum;
import com.haruhi.botServer.constant.event.SubTypeEnum;
import com.haruhi.botServer.dispenser.MessageDispenser;
import com.haruhi.botServer.dispenser.NoticeDispenser;
import com.haruhi.botServer.dto.gocq.response.Message;
import com.haruhi.botServer.thread.pool.policy.ShareRunsPolicy;
import com.haruhi.botServer.utils.ApplicationContextProvider;
import com.haruhi.botServer.ws.Server;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.web.socket.WebSocketSession;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ProcessMessageTask implements Runnable{

    private final WebSocketSession session;
    private final Message message;
    private ProcessMessageTask(WebSocketSession session,Message message){
        this.session = session;
        this.message = message;
    }

    private final static ThreadPoolExecutor threadPool;

    private static final MessageDispenser messageDispenser;
    private static final NoticeDispenser noticeDispenser;
    static {
        messageDispenser = ApplicationContextProvider.getBean(MessageDispenser.class);
        noticeDispenser = ApplicationContextProvider.getBean(NoticeDispenser.class);
        threadPool = new ThreadPoolExecutor(16, 31, 10L * 60L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(160), new CustomizableThreadFactory("pool-processMessage-"), new ShareRunsPolicy("pool-processMessage"));
    }

    @Override
    public void run() {
        try {
            if(PostTypeEnum.message.toString().equals(message.getPostType())){
                // 普通消息
                log.info("[{}]收到来自用户[{}]的消息:{}", message.getMessageType(), message.getUserId(), message.getRawMessage());
                if(message.getRawMessage() != null){
                    messageDispenser.onEvent(session, message);
                }
            }else if(PostTypeEnum.notice.toString().equals(message.getPostType())){
                // bot通知
                noticeDispenser.onEvent(session, message);
            } else if(PostTypeEnum.meta_event.toString().equals(message.getPostType())){
                // 系统消息
                if(MetaEventEnum.lifecycle.toString().equals(message.getMetaEventType()) && SubTypeEnum.connect.toString().equals(message.getSubType())){
                    // 刚连接成功时，gocq会发一条消息给bot
                    Server.setBotIdToCache(session, message.getSelfId());
                }
            }else {
                log.info("未知PostType: {}", message.getPostType());
            }
        }catch (Exception e){
            log.error("处理消息时异常",e);
        }

    }

    public static void execute(WebSocketSession session, Message bean){
        threadPool.execute(new ProcessMessageTask(session,bean));
    }
}
