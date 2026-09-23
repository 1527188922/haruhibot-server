package com.haruhi.botserver.bot.dispatch;

import com.haruhi.botserver.configuration.service.Configs;

import com.haruhi.botserver.configuration.metadata.ConfigKey;

import com.haruhi.botserver.integration.onebot.model.NoticeTypeEnum;
import com.haruhi.botserver.integration.onebot.model.SubTypeEnum;
import com.haruhi.botserver.integration.onebot.model.Message;
import com.haruhi.botserver.bot.handler.IGroupDecreaseHandler;
import com.haruhi.botserver.bot.handler.IGroupIncreaseHandler;
import com.haruhi.botserver.bot.handler.INoticeHandler;
import com.haruhi.botserver.bot.handler.IPokeHandler;
import com.haruhi.botserver.bot.session.Bot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * qq通知分发器
 * 通知将通过这个类分发给所有实现了接口 INoticeEventType 的类
 */
@Slf4j
@Component
public class NoticeDispatcher {

    private final Map<String, INoticeHandler> noticeHandlerMap;

    private final List<INoticeHandler> container = new ArrayList<>();

    public NoticeDispatcher(Map<String, INoticeHandler> noticeHandlerMap) {
        this.noticeHandlerMap = noticeHandlerMap;
    }

    @PostConstruct
    private void loadHandlers(){
        log.info("加载通知处理类...");
        if(!CollectionUtils.isEmpty(noticeHandlerMap)){
            for (INoticeHandler value : noticeHandlerMap.values()) {
                attach(value);
            }
            log.info("加载了{}个通知处理类",container.size());
        }


    }
    public void attach(INoticeHandler handler){
        container.add(handler);
    }

    public void onEvent(final Bot bot, final Message message){
        if(!CollectionUtils.isEmpty(container)){
            String subType = message.getSubType();
            String noticeType = message.getNoticeType();
            boolean disableGroup = Configs.getBool(ConfigKey.BOT_SWITCH_DISABLE_GROUP);
            if(disableGroup && message.isGroupMsg()){
                return;
            }
            log.info("收到通知类消息：subType：{}，noticeType：{}",subType,noticeType);
            if(NoticeTypeEnum.notify.toString().equals(noticeType) && SubTypeEnum.poke.toString().equals(subType)){
                for (INoticeHandler value : container){
                    if(value instanceof IPokeHandler){
                        ((IPokeHandler) value).onPoke(bot,message);
                    }
                }

            }else if(NoticeTypeEnum.group_increase.toString().equals(noticeType)){
                for (INoticeHandler value : container){
                    if(value instanceof IGroupIncreaseHandler){
                        ((IGroupIncreaseHandler) value).onGroupIncrease(bot,message);
                    }
                }
            }else if(NoticeTypeEnum.group_decrease.toString().equals(noticeType)){
                for (INoticeHandler value : container){
                    if (value instanceof IGroupDecreaseHandler) {
                        ((IGroupDecreaseHandler)value).onGroupDecrease(bot,message);
                    }
                }
            }
        }
    }

}
