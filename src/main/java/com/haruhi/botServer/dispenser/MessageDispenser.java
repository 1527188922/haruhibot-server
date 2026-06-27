package com.haruhi.botServer.dispenser;

import com.haruhi.botServer.constant.DictionaryEnum;
import com.haruhi.botServer.dto.qqclient.Message;
import com.haruhi.botServer.handler.message.IGroupMessageHandler;
import com.haruhi.botServer.handler.message.IAllMessageHandler;
import com.haruhi.botServer.handler.message.IMessageHandler;
import com.haruhi.botServer.handler.message.IPrivateMessageHandler;
import com.haruhi.botServer.handler.message.chatRecord.ChatRecordHandler;
import com.haruhi.botServer.service.DictionarySqliteService;
import com.haruhi.botServer.utils.ApplicationContextProvider;
import com.haruhi.botServer.ws.Bot;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import java.util.*;
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

    public MessageDispenser(Map<String, IMessageHandler> map, DictionarySqliteService dictionarySqliteService) {
        this.dictionarySqliteService = dictionarySqliteService;
        loadHandlers(map);
    }

    @Getter
    private static final List<IMessageHandler> container = new CopyOnWriteArrayList<>();
    private static final List<IMessageHandler> groupContainer = new CopyOnWriteArrayList<>();
    private static final List<IMessageHandler> privateContainer = new CopyOnWriteArrayList<>();

    private void loadHandlers(Map<String, IMessageHandler> messageHandlerMap){
        if (messageHandlerMap != null && !messageHandlerMap.isEmpty()) {
            log.info("加载消息处理类...");
            container.addAll(messageHandlerMap.values());
            checkWeight();
            sortByWeight(container);
            groupContainer.addAll(container.stream().filter(e -> e instanceof IGroupMessageHandler || e instanceof IAllMessageHandler).toList());
            privateContainer.addAll(container.stream().filter(e -> e instanceof IPrivateMessageHandler || e instanceof IAllMessageHandler).toList());

            printHandler();
            log.info("加载了{}个消息处理类",container.size());
        }
    }

    public void printHandler(){
        container.forEach(e -> log.info(e.print()));
    }

    private void checkWeight(){
        List<Integer> weights = container.stream().map(IMessageHandler::weight).toList();
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
    public void sortByWeight(List<IMessageHandler> list){
        List<IMessageHandler> collect = list.stream().sorted(Comparator.comparing(IMessageHandler::weight).reversed()).toList();
        list.clear();
        list.addAll(collect);
    }

    /**
     * 对外提供的添加处理类的方法
     * 添加对象必须是IOC容器中存在的对象
     * @param clazz 类模板对象 同一个类的模板对象必为唯一
     * @param <T>
     */
    public <T extends IMessageHandler> void attach(Class<T> clazz){
        T handler = ApplicationContextProvider.getBean(clazz);
        if(handler instanceof IAllMessageHandler){
            if (!groupContainer.contains(handler)) {
                groupContainer.add(handler);
            }
            if (!privateContainer.contains(handler)) {
                privateContainer.add(handler);
            }
        }else if(handler instanceof IPrivateMessageHandler){
            if (!privateContainer.contains(handler)) {
                privateContainer.add(handler);
            }
        }else if (handler instanceof IGroupMessageHandler){
            if (!groupContainer.contains(handler)) {
                groupContainer.add(handler);
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
    public <T extends IMessageHandler> void detach(Class<T> clazz){
        T handler = ApplicationContextProvider.getBean(clazz);
        if(handler instanceof IAllMessageHandler){
            groupContainer.remove(handler);
            privateContainer.remove(handler);
        }else if(handler instanceof IPrivateMessageHandler){
            privateContainer.remove(handler);
        }else if (handler instanceof IGroupMessageHandler){
            groupContainer.remove(handler);
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

    private void executeGroupMessageHandler(List<IMessageHandler> handlers, Bot bot, Message message){
        if(CollectionUtils.isEmpty(handlers)){
            return;
        }
        for (IMessageHandler element : handlers) {
            if (toContinue(element, message)) {
                continue;
            }
            if (element instanceof IAllMessageHandler) {
                IAllMessageHandler handler = (IAllMessageHandler) element;
                if (handler.onMessage(bot, message)) {
                    printLog("onMessage","群",message);
                    break;
                }
            } else if(element instanceof IGroupMessageHandler){
                IGroupMessageHandler handler = (IGroupMessageHandler) element;
                if (handler.onGroup(bot, message)) {
                    printLog("onGroup","群",message);
                    break;
                }
            }
        }
    }


    private void executePrivateMessageHandler(List<IMessageHandler> handlers, Bot bot, Message message){
        if(CollectionUtils.isEmpty(handlers)){
           return;
        }
        for (IMessageHandler element : handlers) {
            if (toContinue(element, message)) {
                continue;
            }
            if (element instanceof IAllMessageHandler) {
                IAllMessageHandler handler = (IAllMessageHandler) element;
                if (handler.onMessage(bot, message)) {
                    printLog("onMessage","私",message);
                    break;
                }
            } else if (element instanceof IPrivateMessageHandler) {
                IPrivateMessageHandler handler = (IPrivateMessageHandler) element;
                if (handler.onPrivate(bot, message)) {
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
     * @param handler
     * @param message
     * @return true:跳过当前handler不执行
     */
    private boolean toContinue(IMessageHandler handler, Message message){
        if(message.isSelfMsg() && !handler.handleSelfMsg()){
            // 机器人self消息 且 handler类不处理self消息
            return true;
        }

        if (message.isGroupMsg()) {
            List<Long> accessGroups = dictionarySqliteService.getList(DictionaryEnum.BOT_ACCESS_GROUP.getKey(), "[,，]", Long.class, Collections.emptyList());
            if(CollectionUtils.isNotEmpty(accessGroups)
                    && !accessGroups.contains(message.getGroupId())
                    && !(handler instanceof ChatRecordHandler)){
                return true;
            }

            boolean disableGroup = dictionarySqliteService.getBoolean(DictionaryEnum.SWITCH_DISABLE_GROUP.getKey(), false);
            if(disableGroup
                    && !(handler instanceof ChatRecordHandler)){
                // 本次为群消息 且开了禁用群功能 则只让聊天记录保存handler类生效
                return true;
            }
        }
        return false;
    }

    /**
     * 查找处理类
     * @param fun 可以是name也可以是id(weight)
     * @return
     */
    public IMessageHandler findHandler(String fun){
        Integer funId;
        IMessageHandler messageEventType;
        try {
            funId = Integer.valueOf(fun);
            messageEventType = findHandlerByWeight(funId);
        }catch (Exception e){
            messageEventType = findHandlerByName(fun);
        }
        return messageEventType;
    }
    private IMessageHandler findHandlerByName(String funName){
        for (IMessageHandler handler : container) {
            if(handler.funName().equals(funName)){
                return handler;
            }
        }
        return null;
    }
    private IMessageHandler findHandlerByWeight(int weight){
        for (IMessageHandler handler : container) {
            if(handler.weight() == (weight)){
                return handler;
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
    public <T extends IMessageHandler> boolean exist(Class<T> tClass){
        T bean = ApplicationContextProvider.getBean(tClass);
        return groupContainer.contains(bean) || privateContainer.contains(bean);
    }
}
