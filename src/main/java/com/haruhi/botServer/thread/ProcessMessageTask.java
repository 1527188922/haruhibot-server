package com.haruhi.botServer.thread;

import com.alibaba.fastjson.JSONObject;
import com.haruhi.botServer.constant.event.MetaEventEnum;
import com.haruhi.botServer.constant.event.PostTypeEnum;
import com.haruhi.botServer.constant.event.SubTypeEnum;
import com.haruhi.botServer.dispenser.MessageDispenser;
import com.haruhi.botServer.dispenser.NoticeDispenser;
import com.haruhi.botServer.dto.gocq.response.Message;
import com.haruhi.botServer.thread.pool.policy.ShareRunsPolicy;
import com.haruhi.botServer.utils.ApplicationContextProvider;
import com.haruhi.botServer.utils.GocqSyncRequestUtil;
import com.haruhi.botServer.ws.Server;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.web.socket.WebSocketSession;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ProcessMessageTask implements Runnable{

    private final WebSocketSession session;
    private final Message bean;
    private final String original;
    private ProcessMessageTask(WebSocketSession session,Message bean,String original){
        this.session = session;
        this.bean = bean;
        this.original = original;
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
            if(PostTypeEnum.message.toString().equals(bean.getPostType())){
                // 普通消息
                final String command = bean.getRawMessage();
                log.info("[{}]收到来自用户[{}]的消息:{}",bean.getMessageType(),bean.getUserId(),command);
                if(command != null){
                    messageDispenser.onEvent(session,bean,command);
                }
            }else if(PostTypeEnum.notice.toString().equals(bean.getPostType())){
                // bot通知
                noticeDispenser.onEvent(session,bean);
            } else if(PostTypeEnum.meta_event.toString().equals(bean.getPostType())){
                // 系统消息
                if(MetaEventEnum.lifecycle.toString().equals(bean.getMetaEventType()) && SubTypeEnum.connect.toString().equals(bean.getSubType())){
                    // 刚连接成功时，gocq会发一条消息给bot
                    Server.putUserIdMap(session.getId(), bean.getSelfId());
                }
            }else {
                JSONObject jsonObject = JSONObject.parseObject(original);
                String echo = jsonObject.getString("echo");
                if (Strings.isNotBlank(echo)) {
                    GocqSyncRequestUtil.putEchoResult(echo,jsonObject);
                }
            }
        }catch (Exception e){
            log.error("处理消息时异常",e);
        }

    }

    public static void execute(final WebSocketSession session,final Message bean,final String original){
        threadPool.execute(new ProcessMessageTask(session,bean,original));
    }
}
