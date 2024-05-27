package com.haruhi.botServer.dispenser;

import com.haruhi.botServer.config.SwitchConfig;
import com.haruhi.botServer.constant.event.NoticeTypeEnum;
import com.haruhi.botServer.constant.event.SubTypeEnum;
import com.haruhi.botServer.dto.gocq.response.Message;
import com.haruhi.botServer.event.notice.IGroupDecreaseEvent;
import com.haruhi.botServer.event.notice.IGroupIncreaseEvent;
import com.haruhi.botServer.event.notice.INoticeEvent;
import com.haruhi.botServer.event.notice.IPokeEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.socket.WebSocketSession;

import javax.annotation.PostConstruct;
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

    private final Map<String, INoticeEvent> noticeEventMap;

    private static List<INoticeEvent> container = new ArrayList<>();

    public NoticeDispenser(Map<String, INoticeEvent> noticeEventMap) {
        this.noticeEventMap = noticeEventMap;
    }

    @PostConstruct
    private void loadEvent(){
        log.info("加载通知处理类...");
        if(!CollectionUtils.isEmpty(noticeEventMap)){
            for (INoticeEvent value : noticeEventMap.values()) {
                attach(value);
            }
            log.info("加载了{}个通知处理类",container.size());
        }


    }
    public void attach(INoticeEvent event){
        container.add(event);
    }

    public void onEvent(final WebSocketSession session,final Message message){
        if(!CollectionUtils.isEmpty(container)){
            String subType = message.getSubType();
            String noticeType = message.getNoticeType();
            if(SwitchConfig.DISABLE_GROUP && message.isGroupMsg()){
                return;
            }
            log.info("收到通知类消息：subType：{}，noticeType：{}",subType,noticeType);
            if(NoticeTypeEnum.notify.toString().equals(noticeType) && SubTypeEnum.poke.toString().equals(subType)){
                for (INoticeEvent value : container){
                    if(value instanceof IPokeEvent){
                        ((IPokeEvent) value).onPoke(session,message);
                    }
                }

            }else if(NoticeTypeEnum.group_increase.toString().equals(noticeType)){
                for (INoticeEvent value : container){
                    if(value instanceof IGroupIncreaseEvent){
                        ((IGroupIncreaseEvent) value).onGroupIncrease(session,message);
                    }
                }
            }else if(NoticeTypeEnum.group_decrease.toString().equals(noticeType)){
                for (INoticeEvent value : container){
                    if (value instanceof IGroupDecreaseEvent) {
                        ((IGroupDecreaseEvent)value).onGroupDecrease(session,message);
                    }
                }
            }
        }
    }

}
