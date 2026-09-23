package com.haruhi.botserver.bot.dispatch;

import com.haruhi.botserver.shared.constant.BusinessModuleEnum;
import com.haruhi.botserver.integration.onebot.model.MetaEventEnum;
import com.haruhi.botserver.integration.onebot.model.PostTypeEnum;
import com.haruhi.botserver.integration.onebot.model.SubTypeEnum;
import com.haruhi.botserver.integration.onebot.model.Message;
import com.haruhi.botserver.features.contacts.service.FriendSqliteService;
import com.haruhi.botserver.features.contacts.service.GroupInfoSqliteService;
import com.haruhi.botserver.infrastructure.logging.DbLog;
import com.haruhi.botserver.infrastructure.concurrent.ThreadPoolUtil;
import com.haruhi.botserver.bot.session.Bot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Slf4j
@Component
public class MessageProcessor{


    @Autowired
    private MessageDispatcher messageDispenser;
    @Autowired
    private NoticeDispatcher noticeDispenser;
    @Autowired
    private GroupInfoSqliteService groupInfoSqliteService;
    @Autowired
    private FriendSqliteService friendSqliteService;


    public void execute(Bot bot, Message message){

        ThreadPoolUtil.getHandleCommandPool().execute(()->{
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
                DbLog.error(BusinessModuleEnum.BOT_WS,"处理消息时异常",e);
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

            // 刷新bot信息
            bot.refreshSelfInfo();
            try {
                // 加载群信息
                groupInfoSqliteService.loadGroupInfo(bot);
                friendSqliteService.loadFriendInfo(bot);
            }catch (Exception e){
                log.error("初始加载机器人群聊异常 bot：{}",message.getSelfId(),e);
            }

        }
    }
}
