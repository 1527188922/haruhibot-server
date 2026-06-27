package com.haruhi.botServer.dispenser;

import com.haruhi.botServer.constant.DictionaryEnum;
import com.haruhi.botServer.constant.event.NoticeTypeEnum;
import com.haruhi.botServer.constant.event.SubTypeEnum;
import com.haruhi.botServer.dto.qqclient.Message;
import com.haruhi.botServer.handler.notice.IGroupDecreaseHandler;
import com.haruhi.botServer.handler.notice.IGroupIncreaseHandler;
import com.haruhi.botServer.handler.notice.INoticeHandler;
import com.haruhi.botServer.handler.notice.IPokeHandler;
import com.haruhi.botServer.service.DictionarySqliteService;
import com.haruhi.botServer.ws.Bot;
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
public class NoticeDispenser {

    private final Map<String, INoticeHandler> noticeHandlerMap;

    private static List<INoticeHandler> container = new ArrayList<>();
    private final DictionarySqliteService dictionarySqliteService;

    public NoticeDispenser(Map<String, INoticeHandler> noticeHandlerMap, DictionarySqliteService dictionarySqliteService) {
        this.noticeHandlerMap = noticeHandlerMap;
        this.dictionarySqliteService = dictionarySqliteService;
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
            boolean disableGroup = dictionarySqliteService.getBoolean(DictionaryEnum.SWITCH_DISABLE_GROUP.getKey(), false);
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
