package com.haruhi.botServer.dispenser;

import com.haruhi.botServer.config.SwitchConfig;
import com.haruhi.botServer.dto.gocq.response.Message;
import com.haruhi.botServer.event.message.IGroupMessageEvent;
import com.haruhi.botServer.event.message.IAllMessageEvent;
import com.haruhi.botServer.event.message.IMessageEvent;
import com.haruhi.botServer.event.message.IPrivateMessageEvent;
import com.haruhi.botServer.handlers.message.chatRecord.SavaChatRecordHandler;
import com.haruhi.botServer.utils.ApplicationContextProvider;
import lombok.extern.slf4j.Slf4j;
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

    private static Map<String, IMessageEvent> messageEventMap;

    public MessageDispenser(Map<String, IMessageEvent> messageEventMap) {
        MessageDispenser.messageEventMap = messageEventMap;
    }

    public static Map<String, IMessageEvent> getMessageEventMap(){
        return messageEventMap;
    }

    private static List<IMessageEvent> container = new CopyOnWriteArrayList<>();


    @PostConstruct
    private void loadEvent(){
        if (!CollectionUtils.isEmpty(messageEventMap)) {
            log.info("加载消息处理类...");
            for (IMessageEvent value : messageEventMap.values()) {
                attach(value);
            }
            checkWeight();
            int size = sortByWeight();
            printHandler();
            log.info("加载了{}个消息处理类",size);
        }
    }

    public void printHandler(){
        container.forEach(e -> log.info(e.print()));
    }

    private void checkWeight(){
        List<Integer> weights = container.stream().map(IMessageEvent::weight).collect(Collectors.toList());
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
    public int sortByWeight(){
        container = container.stream().sorted(Comparator.comparing(IMessageEvent::weight).reversed()).collect(Collectors.toList());
        return container.size();
    }

    private <T extends IMessageEvent> void attach(T event){
        container.add(event);
    }

    /**
     * 对外提供的添加处理类的方法
     * 添加对象必须是IOC容器中存在的对象
     * @param clazz 类模板对象 同一个类的模板对象必为唯一
     * @param <T>
     */
    public <T extends IMessageEvent> void attach(Class<T> clazz){
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
    public <T extends IMessageEvent> void detach(Class<T> clazz){
        T bean = ApplicationContextProvider.getBean(clazz);
        container.remove(bean);
    }

    public void onEvent(WebSocketSession session, Message message){
        if (!CollectionUtils.isEmpty(container)) {
            if (message.isGroupMsg()) {
                executeGroupMessageHandler(container,session,message);
            } else if (message.isPrivateMsg()) {
                executePrivateMessageHandler(container,session,message);
            }
        }
    }

    private void executeGroupMessageHandler(List<IMessageEvent> events, WebSocketSession session, Message message){
        for (IMessageEvent element : events) {
            if(SwitchConfig.DISABLE_GROUP && element.getClass() != SavaChatRecordHandler.class){
                continue;
            }
            if (element instanceof IAllMessageEvent) {
                IAllMessageEvent event = (IAllMessageEvent) element;
                if (event.onMessage(session, message)) {
                    break;
                }
            } else if(element instanceof IGroupMessageEvent){
                IGroupMessageEvent event = (IGroupMessageEvent) element;
                if (event.onGroup(session, message)) {
                    break;
                }
            }
        }
    }


    private void executePrivateMessageHandler(List<IMessageEvent> events, WebSocketSession session, Message message){
        for (IMessageEvent element : events) {
            if (element instanceof IAllMessageEvent) {
                IAllMessageEvent event = (IAllMessageEvent) element;
                if (event.onMessage(session, message)) {
                    break;
                }
            } else if (element instanceof IPrivateMessageEvent) {
                IPrivateMessageEvent event = (IPrivateMessageEvent) element;
                if (event.onPrivate(session, message)) {
                    break;
                }
            }
        }
    }

    /**
     * 查找处理类
     * @param fun 可以是name也可以是id(weight)
     * @return
     */
    public IMessageEvent findHandler(String fun){
        Integer funId;
        IMessageEvent messageEventType;
        try {
            funId = Integer.valueOf(fun);
            messageEventType = findHandlerByWeight(funId);
        }catch (Exception e){
            messageEventType = findHandlerByName(fun);
        }
        return messageEventType;
    }
    private IMessageEvent findHandlerByName(String funName){
        for (Map.Entry<String, IMessageEvent> eventTypeEntry : messageEventMap.entrySet()) {
            if(eventTypeEntry.getValue().funName().equals(funName)){
                return eventTypeEntry.getValue();
            }
        }
        return null;
    }
    private IMessageEvent findHandlerByWeight(int weight){
        for (Map.Entry<String, IMessageEvent> eventTypeEntry : messageEventMap.entrySet()) {
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
    public <T extends IMessageEvent> boolean exist(Class<T> tClass){
        T bean = ApplicationContextProvider.getBean(tClass);
        return container.contains(bean);
    }
}
