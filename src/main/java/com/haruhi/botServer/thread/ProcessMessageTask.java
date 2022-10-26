package com.haruhi.botServer.thread;

import com.alibaba.fastjson.JSONObject;
import com.haruhi.botServer.constant.event.MetaEventEnum;
import com.haruhi.botServer.constant.event.PostTypeEnum;
import com.haruhi.botServer.constant.event.SubTypeEnum;
import com.haruhi.botServer.dispenser.MessageDispenser;
import com.haruhi.botServer.dispenser.NoticeDispenser;
import com.haruhi.botServer.dto.gocq.response.Message;
import com.haruhi.botServer.utils.GocqSyncRequestUtil;
import com.haruhi.botServer.ws.Server;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.web.socket.WebSocketSession;

@Slf4j
public class ProcessMessageTask implements Runnable{

    private WebSocketSession session;
    private Message bean;
    private String original;
    public ProcessMessageTask(WebSocketSession session,Message bean,String original){
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
}
