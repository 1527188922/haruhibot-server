package com.haruhi.botServer.thread;

import com.alibaba.fastjson.JSONObject;
import com.haruhi.botServer.config.BotConfig;
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
                    Server.sendPrivateMessage(session,bean.getSelf_id(),
                            "欢迎您使用haruhi公共机器人！（以下简称‘haruhi’）\n" +
                                    "注意：由于连上来的gocq共用同一个haruhi后端，所以，一个群不要存在两个及以上同后端的haruhi！" +
                                    "以免给您带来不好的体验！\n" +
                                    "当前haruhi后端地址：" + BotConfig.CONTEXT_PATH + "\n" +
                                    "如果群里已经存在haruhi，您可以与对方确认后端地址是否一致来判断是否为同后端",
                            true);
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
