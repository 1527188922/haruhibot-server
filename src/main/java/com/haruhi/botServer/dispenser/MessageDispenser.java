package com.haruhi.botServer.dispenser;

import com.haruhi.botServer.constant.event.MessageTypeEnum;
import com.haruhi.botServer.dto.gocq.response.Message;
import com.haruhi.botServer.event.message.IGroupMessageEvent;
import com.haruhi.botServer.event.message.IMessageEvent;
import com.haruhi.botServer.event.message.IMessageEventType;
import com.haruhi.botServer.event.message.IPrivateMessageEvent;
import com.haruhi.botServer.utils.ApplicationContextProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.socket.WebSocketSession;

import javax.annotation.PostConstruct;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * 普通消息分发器
 * 收到群聊 私聊消息时
 * 消息将通过这个类分发给所有实现了接口 IMessageEventType 的类
 */
@Slf4j
@Component
public class MessageDispenser {

    private static Map<String, IMessageEventType> messageEventTypeMap;
    public static Map<String, IMessageEventType> getMessageEventTypeMap(){
        return messageEventTypeMap;
    }
    @Autowired
    public void setMessageEventTypeMap(Map<String, IMessageEventType> messageEventTypeMap){
        MessageDispenser.messageEventTypeMap = messageEventTypeMap;
    }
    private static List<IMessageEventType> container = new CopyOnWriteArrayList<>();


    @PostConstruct
    private void loadEvent(){
        if (!CollectionUtils.isEmpty(messageEventTypeMap)) {
            log.info("加载消息处理类...");
            for (IMessageEventType value : messageEventTypeMap.values()) {
                MessageDispenser.attach(value);
            }
            checkWeight();
            int size = sortByWeight();
            log.info("加载了{}个消息处理类",size);
        }
    }

    private void checkWeight(){
        List<Integer> weights = container.stream().map(IMessageEventType::weight).collect(Collectors.toList());
        Set<Integer> weightSet = new HashSet<>(weights);
        if(weightSet.size() != weights.size()){
            throw new RuntimeException("Duplicate weight appear");
        }
    }

    /**
     * 根据权重排序
     * 降序
     * @return
     */
    public static int sortByWeight(){
        container = container.stream().sorted(Comparator.comparing(IMessageEventType::weight).reversed()).collect(Collectors.toList());
        return container.size();
    }

    private static <T extends IMessageEventType> void attach(T event){
        container.add(event);
    }

    /**
     * 对外提供的添加处理类的方法
     * 添加对象必须是IOC容器中存在的对象
     * @param clazz 类模板对象 同一个类的模板对象必为唯一
     * @param <T>
     */
    public static <T extends IMessageEventType> void attach(Class<T> clazz){
        T bean = ApplicationContextProvider.getBean(clazz);
        container.add(bean);
    }

    /**
     * 用于从容器中删除消息处理类
     * 可以实现禁用某命令/功能
     * 删除对象必须是IOC容器中存在的对象
     * @param clazz
     * @param <T>
     */
    public static <T extends IMessageEventType> void detach(Class<T> clazz){
        T bean = ApplicationContextProvider.getBean(clazz);
        container.remove(bean);
    }


    public static void onEvent(final WebSocketSession session,final Message message, final String command){
        if (!CollectionUtils.isEmpty(container)) {
            String messageType = message.getMessage_type();

            if(MessageTypeEnum.group.getType().equals(messageType)){
                for (IMessageEventType element : container){
                    if(element instanceof IMessageEvent){
                        IMessageEvent event = (IMessageEvent) element;
                        if(event.onMessage(session,message,command)){
                            break;
                        }
                    }else if(element instanceof IGroupMessageEvent){
                        IGroupMessageEvent event = (IGroupMessageEvent) element;
                        if(event.onGroup(session,message,command)){
                            break;
                        }
                    }
                }
            }else if(MessageTypeEnum.privat.getType().equals(messageType)){
                for (IMessageEventType element : container){
                    if(element instanceof IMessageEvent){
                        IMessageEvent event = (IMessageEvent) element;
                        if(event.onMessage(session,message,command)){
                            break;
                        }
                    }else if(element instanceof IPrivateMessageEvent){
                        IPrivateMessageEvent event = (IPrivateMessageEvent) element;
                        if(event.onPrivate(session,message,command)){
                            break;
                        }
                    }
                }
            }
        }

    }

    /**
     * 查找处理类
     * @param fun 可以是name也可以是id(weight)
     * @return
     */
    public static IMessageEventType findHandler(String fun){
        Integer funId;
        IMessageEventType messageEventType;
        try {
            funId = Integer.valueOf(fun);
            messageEventType = findHandlerByWeight(funId);
        }catch (Exception e){
            messageEventType = findHandlerByName(fun);
        }
        return messageEventType;
    }
    private static IMessageEventType findHandlerByName(String funName){
        for (Map.Entry<String, IMessageEventType> eventTypeEntry : messageEventTypeMap.entrySet()) {
            if(eventTypeEntry.getValue().funName().equals(funName)){
                return eventTypeEntry.getValue();
            }
        }
        return null;
    }
    private static IMessageEventType findHandlerByWeight(int weight){
        for (Map.Entry<String, IMessageEventType> eventTypeEntry : messageEventTypeMap.entrySet()) {
            if(eventTypeEntry.getValue().weight() == weight){
                return eventTypeEntry.getValue();
            }
        }
        return null;
    }

    /**
     * 判断当前功能是否存在
     * 不存在表示全局禁用中
     * @param tClass
     * @param <T>
     * @return
     */
    public static <T extends IMessageEventType> boolean exist(Class<T> tClass){
        T bean = ApplicationContextProvider.getBean(tClass);
        return container.contains(bean);
    }
}
