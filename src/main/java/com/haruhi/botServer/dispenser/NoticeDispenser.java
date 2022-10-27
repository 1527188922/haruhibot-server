package com.haruhi.botServer.dispenser;

import com.haruhi.botServer.constant.event.MessageTypeEnum;
import com.haruhi.botServer.constant.event.NoticeTypeEnum;
import com.haruhi.botServer.constant.event.SubTypeEnum;
import com.haruhi.botServer.dto.gocq.response.Message;
import com.haruhi.botServer.event.notice.IGroupDecreaseEvent;
import com.haruhi.botServer.event.notice.IGroupIncreaseEvent;
import com.haruhi.botServer.event.notice.INoticeEventType;
import com.haruhi.botServer.event.notice.IPokeEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
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

    private static Map<String, INoticeEventType> noticeEventTypeMap;
    @Autowired
    public void setMessageEventTypeMap(Map<String, INoticeEventType> pokeEventMap){
        NoticeDispenser.noticeEventTypeMap = pokeEventMap;
    }
    private static List<INoticeEventType> container = new ArrayList<>();

    @PostConstruct
    private void loadEvent(){
        log.info("加载通知处理类...");
        if(!CollectionUtils.isEmpty(noticeEventTypeMap)){
            for (INoticeEventType value : noticeEventTypeMap.values()) {
                NoticeDispenser.attach(value);
            }
            log.info("加载了{}个通知处理类",container.size());
        }


    }
    public static void attach(INoticeEventType event){
        container.add(event);
    }

    public static void onEvent(final WebSocketSession session,final Message message){
        if(!CollectionUtils.isEmpty(container)){
            setMessageType(message);
            String subType = message.getSub_type();
            String noticeType = message.getNotice_type();
            log.info("收到通知类消息：subType：{}，noticeType：{}",subType,noticeType);
            if(NoticeTypeEnum.notify.toString().equals(noticeType) && SubTypeEnum.poke.toString().equals(subType)){
                for (INoticeEventType value : container){
                    if(value instanceof IPokeEvent){
                        ((IPokeEvent) value).onPoke(session,message);
                    }
                }

            }else if(NoticeTypeEnum.group_increase.toString().equals(noticeType)){
                for (INoticeEventType value : container){
                    if(value instanceof IGroupIncreaseEvent){
                        ((IGroupIncreaseEvent) value).onGroupIncrease(session,message);
                    }
                }
            }else if(NoticeTypeEnum.group_decrease.toString().equals(noticeType)){
                for (INoticeEventType value : container){
                    if (value instanceof IGroupDecreaseEvent) {
                        ((IGroupDecreaseEvent)value).onGroupDecrease(session,message);
                    }
                }
            }
        }
    }

    private static void setMessageType(final Message message){
        if(Strings.isBlank(message.getMessage_type())){
            if(message.getGroup_id() != null){
                message.setMessage_type(MessageTypeEnum.group.getType());
            }else if(message.getUser_id() != null){
                message.setMessage_type(MessageTypeEnum.privat.getType());
            }
        }
    }
}
