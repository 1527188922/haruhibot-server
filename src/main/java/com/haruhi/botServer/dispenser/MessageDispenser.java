package com.haruhi.botServer.dispenser;

import com.haruhi.botServer.constant.event.MessageEventEnum;
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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
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

    /**
     * 群禁用的功能
     * key:群号
     * value:禁用的消息处理类的类名集合
     * element:类名(全路径)
     */
    private static Map<String,List<String>> groupBanFunction = new ConcurrentHashMap<>(0);
    public static <T extends IMessageEventType> void setGroupBanFunction(String groupId,Class<T> tClass){
        setGroupBanFunction(groupId,tClass.getName());
    }
    private static void setGroupBanFunction(String groupId,String className){
        if(groupBanFunction.containsKey(groupId)){
            List<String> classNames = groupBanFunction.get(groupId);
            classNames.add(className);
        }else{
            List<String> classNames = new ArrayList<>(1);
            classNames.add(className);
            groupBanFunction.put(groupId,classNames);
        }
    }
    public static void setGroupBanFunction(Map<String,List<String>> var){
        groupBanFunction = var;
    }
    public static <T extends IMessageEventType> void groupUnbanFunction(String groupId,Class<T> tClass){
        groupUnbanFunction(groupId,tClass.getName());
    }
    private static void groupUnbanFunction(String groupId,String className){
        if(groupBanFunction.containsKey(groupId)){
            List<String> classNames = groupBanFunction.get(groupId);
            if(!CollectionUtils.isEmpty(classNames)){
                classNames.remove(className);
            }
        }
    }
    /**
     * 虽没被引用
     * 但不可删除
     */
    @Autowired
    private ApplicationContextProvider applicationContextProvider;
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

            if(MessageEventEnum.group.getType().equals(messageType)){
                for (IMessageEventType element : container){
                    if(element instanceof IMessageEvent){
                        IMessageEvent event = (IMessageEvent) element;
                        if(isBanFunctionByGroup(event,message.getGroup_id())){
                            continue;
                        }
                        if(event.onMessage(session,message,command)){
                            break;
                        }
                    }else if(element instanceof IGroupMessageEvent){
                        IGroupMessageEvent event = (IGroupMessageEvent) element;
                        if(isBanFunctionByGroup(event,message.getGroup_id())){
                            continue;
                        }
                        if(event.onGroup(session,message,command)){
                            break;
                        }
                    }
                }
            }else if(MessageEventEnum.privat.getType().equals(messageType)){
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
     * 群功能是否被禁用
     * @param event
     * @param groupId
     * @return
     */
    private static boolean isBanFunctionByGroup(IMessageEventType event,Long groupId){
        return isBanFunctionByGroup(event.getClass(),groupId);
    }

    /**
     * 公开方法
     * @param tClass 对外提供的isBanFunctionByGroup方法,不能直接传IMessageEventType对象 必须为Class对象
     * @param groupId
     * @param <T>
     * @return
     */
    public static <T extends IMessageEventType> boolean isBanFunctionByGroup(Class<T> tClass,Long groupId){
        if(groupBanFunction.size() == 0){
            return false;
        }
        if (!groupBanFunction.containsKey(groupId)) {
            return false;
        }
        List<String> classPaths = groupBanFunction.get(groupId);
        if(CollectionUtils.isEmpty(classPaths)){
            return false;
        }
        return classPaths.contains(tClass.getName());
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
