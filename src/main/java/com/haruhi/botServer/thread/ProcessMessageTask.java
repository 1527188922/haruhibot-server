package com.haruhi.botServer.thread;

import com.alibaba.fastjson.JSONObject;
import com.haruhi.botServer.constant.event.MetaEventEnum;
import com.haruhi.botServer.constant.event.PostTypeEnum;
import com.haruhi.botServer.constant.event.SubTypeEnum;
import com.haruhi.botServer.dispenser.MessageDispenser;
import com.haruhi.botServer.dispenser.NoticeDispenser;
import com.haruhi.botServer.dto.gocq.response.Message;
import com.haruhi.botServer.utils.ThreadPoolUtil;
import com.haruhi.botServer.utils.GocqSyncRequestUtil;
import com.haruhi.botServer.ws.Server;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.web.socket.WebSocketSession;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ProcessMessageTask implements Runnable{

    private final static ThreadPoolExecutor threadPool = new ThreadPoolExecutor(16, 31, 10L * 60L, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(160), new CustomizableThreadFactory("pool-processMessage-"), new RejectedExecutionHandler() {
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            ThreadPoolUtil.getSharePool().execute(r);
            log.error("线程池：pool-processMessage任务队列已满，本次消息处理由公共线程池执行");
        }
    });

    private WebSocketSession session;
    private Message bean;
    private String original;
    private ProcessMessageTask(WebSocketSession session,Message bean,String original){
        this.session = session;
        this.bean = bean;
        this.original = original;
    }

    @Override
    public void run() {
        try {
            if(PostTypeEnum.message.toString().equals(bean.getPost_type())){
                // 普通消息
                final String command = bean.getMessage();
                log.info("[{}]收到来自用户[{}]的消息:{}",bean.getMessage_type(),bean.getUser_id(),command);
                if(command != null){
                    MessageDispenser.onEvent(session,bean,command);
                }
            }else if(PostTypeEnum.notice.toString().equals(bean.getPost_type())){
                // bot通知
                NoticeDispenser.onEvent(session,bean);
            } else if(PostTypeEnum.meta_event.toString().equals(bean.getPost_type())){
                // 系统消息
                if(MetaEventEnum.lifecycle.toString().equals(bean.getMeta_event_type()) && SubTypeEnum.connect.toString().equals(bean.getSub_type())){
                    // 刚连接成功时，gocq会发一条消息给bot
                    Server.putUserIdMap(session.getId(), bean.getSelf_id());
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

    public static void execute(WebSocketSession session,Message bean,String original){
        threadPool.execute(new ProcessMessageTask(session,bean,original));
    }
}
