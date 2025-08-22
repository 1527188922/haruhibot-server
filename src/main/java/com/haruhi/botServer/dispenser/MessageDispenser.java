package com.haruhi.botServer.dispenser;

import com.haruhi.botServer.constant.DictionaryEnum;
import com.haruhi.botServer.dto.qqclient.Message;
import com.haruhi.botServer.event.message.IGroupMessageEvent;
import com.haruhi.botServer.event.message.IAllMessageEvent;
import com.haruhi.botServer.event.message.IMessageEvent;
import com.haruhi.botServer.event.message.IPrivateMessageEvent;
import com.haruhi.botServer.handlers.message.chatRecord.ChatRecordHandler;
import com.haruhi.botServer.service.DictionarySqliteService;
import com.haruhi.botServer.utils.ApplicationContextProvider;
import com.haruhi.botServer.ws.Bot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

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


    private final DictionarySqliteService dictionarySqliteService;

    public MessageDispenser(Map<String, IMessageEvent> map, DictionarySqliteService dictionarySqliteService) {
        this.dictionarySqliteService = dictionarySqliteService;
        loadEvent(map);
    }

    private static final List<IMessageEvent> container = new CopyOnWriteArrayList<>();
    private static final List<IMessageEvent> groupContainer = new CopyOnWriteArrayList<>();
    private static final List<IMessageEvent> privateContainer = new CopyOnWriteArrayList<>();

    public static List<IMessageEvent> getContainer(){
        return container;
    }

    private void loadEvent(Map<String, IMessageEvent> messageEventMap){
        if (!CollectionUtils.isEmpty(messageEventMap)) {
            log.info("加载消息处理类...");
            container.addAll(messageEventMap.values());
            checkWeight();
            sortByWeight(container);
            groupContainer.addAll(container.stream().filter(e -> e instanceof IGroupMessageEvent || e instanceof IAllMessageEvent).collect(Collectors.toList()));
            privateContainer.addAll(container.stream().filter(e -> e instanceof IPrivateMessageEvent || e instanceof IAllMessageEvent).collect(Collectors.toList()));

            printHandler();
            log.info("加载了{}个消息处理类",container.size());
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
    public void sortByWeight(List<IMessageEvent> list){
        List<IMessageEvent> collect = list.stream().sorted(Comparator.comparing(IMessageEvent::weight).reversed()).collect(Collectors.toList());
        list.clear();
        list.addAll(collect);
    }

    /**
     * 对外提供的添加处理类的方法
     * 添加对象必须是IOC容器中存在的对象
     * @param clazz 类模板对象 同一个类的模板对象必为唯一
     * @param <T>
     */
    public <T extends IMessageEvent> void attach(Class<T> clazz){
        T event = ApplicationContextProvider.getBean(clazz);
        if(event instanceof IAllMessageEvent){
            if (!groupContainer.contains(event)) {
                groupContainer.add(event);
            }
            if (!privateContainer.contains(event)) {
                privateContainer.add(event);
            }
        }else if(event instanceof IPrivateMessageEvent){
            if (!privateContainer.contains(event)) {
                privateContainer.add(event);
            }
        }else if (event instanceof IGroupMessageEvent){
            if (!groupContainer.contains(event)) {
                groupContainer.add(event);
            }
        }
    }

    /**
     * 用于从容器中删除消息处理类
     * 可以实现禁用某命令/功能
     * 删除对象必须是IOC容器中存在的对象
     * @param clazz
     * @param <T>
     */
    public <T extends IMessageEvent> void detach(Class<T> clazz){
        T event = ApplicationContextProvider.getBean(clazz);
        if(event instanceof IAllMessageEvent){
            groupContainer.remove(event);
            privateContainer.remove(event);
        }else if(event instanceof IPrivateMessageEvent){
            privateContainer.remove(event);
        }else if (event instanceof IGroupMessageEvent){
            groupContainer.remove(event);
        }
    }

    public void onEvent(Bot bot, Message message){
        if (CollectionUtils.isEmpty(container)) {
            return;
        }
        if (message.isGroupMsg()) {
            executeGroupMessageHandler(groupContainer,bot,message);
        } else if (message.isPrivateMsg()) {
            executePrivateMessageHandler(privateContainer,bot,message);
        }
    }

    private void executeGroupMessageHandler(List<IMessageEvent> events, Bot bot, Message message){
        if(CollectionUtils.isEmpty(events)){
            return;
        }
        for (IMessageEvent element : events) {
            if (toContinue(element, message)) {
                continue;
            }
            if (element instanceof IAllMessageEvent) {
                IAllMessageEvent event = (IAllMessageEvent) element;
                if (event.onMessage(bot, message)) {
                    printLog("onMessage","群",message);
                    break;
                }
            } else if(element instanceof IGroupMessageEvent){
                IGroupMessageEvent event = (IGroupMessageEvent) element;
                if (event.onGroup(bot, message)) {
                    printLog("onGroup","群",message);
                    break;
                }
            }
        }
    }


    private void executePrivateMessageHandler(List<IMessageEvent> events, Bot bot, Message message){
        if(CollectionUtils.isEmpty(events)){
           return;
        }
        for (IMessageEvent element : events) {
            if (toContinue(element, message)) {
                continue;
            }
            if (element instanceof IAllMessageEvent) {
                IAllMessageEvent event = (IAllMessageEvent) element;
                if (event.onMessage(bot, message)) {
                    printLog("onMessage","私",message);
                    break;
                }
            } else if (element instanceof IPrivateMessageEvent) {
                IPrivateMessageEvent event = (IPrivateMessageEvent) element;
                if (event.onPrivate(bot, message)) {
                    printLog("onPrivate","私",message);
                    break;
                }
            }
        }
    }

    private void printLog(String fnName,String messageType, Message message){
        log.info("[{}][{}][UID:{}][GID:{}] {}",messageType,fnName,message.getUserId(),message.getGroupId(),message.getRawMessage());
    }

    /**
     *
     * @param event
     * @param message
     * @return true:跳过当前handler不执行
     */
    private boolean toContinue(IMessageEvent event, Message message){
        if(message.isSelfMsg() && !event.handleSelfMsg()){
            // 机器人self消息 且 handler类不处理self消息
            return true;
        }
        boolean disableGroup = dictionarySqliteService.getBoolean(DictionaryEnum.SWITCH_DISABLE_GROUP.getKey(), false);
        if(message.isGroupMsg() && disableGroup && event.getClass() != ChatRecordHandler.class){
            // 本次为群消息 且开了禁用群功能 则只让聊天记录保存handler类生效
            return true;
        }
        return false;
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
        for (IMessageEvent event : container) {
            if(event.funName().equals(funName)){
                return event;
            }
        }
        return null;
    }
    private IMessageEvent findHandlerByWeight(int weight){
        for (IMessageEvent event : container) {
            if(event.weight() == (weight)){
                return event;
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
        return groupContainer.contains(bean) || privateContainer.contains(bean);
    }
}
