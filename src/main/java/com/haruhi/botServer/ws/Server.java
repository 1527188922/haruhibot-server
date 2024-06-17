package com.haruhi.botServer.ws;

import com.alibaba.fastjson.JSONObject;
import com.haruhi.botServer.config.BotConfig;
import com.haruhi.botServer.constant.GocqActionEnum;
import com.haruhi.botServer.constant.event.MessageTypeEnum;
import com.haruhi.botServer.constant.event.MetaEventEnum;
import com.haruhi.botServer.constant.event.PostTypeEnum;
import com.haruhi.botServer.dto.gocq.request.ForwardMsgItem;
import com.haruhi.botServer.dto.gocq.request.Params;
import com.haruhi.botServer.dto.gocq.request.RequestBox;
import com.haruhi.botServer.dto.gocq.response.Message;
import com.haruhi.botServer.dto.gocq.response.SyncResponse;
import com.haruhi.botServer.thread.ProcessMessageTask;
import com.haruhi.botServer.utils.WsSyncRequestUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.util.CollectionUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 机器人是服务端
 * gocq（客户端）使用反向ws连接
 */
@Slf4j
public class Server extends TextWebSocketHandler {
    
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    private static class UserSession {
        private Long botId;
        private WebSocketSession session;
    }

    // Map<sessionId,{botId,session}>
    private static final Map<String,UserSession> sessionCache = new ConcurrentHashMap<>();

    public static int getConnections(){
        return sessionCache.size();
    }
    @Override
    public void afterConnectionEstablished(final WebSocketSession session) throws Exception {
        sessionCache.put(session.getId(),new UserSession(null,session));
        
        log.info("客户端连接成功,sessionId:{}，客户端数量：{}", session.getId(),getConnections());
    }

    @Override
    public void handleTextMessage(final WebSocketSession session, final TextMessage message) throws Exception {
        final String s = message.getPayload();
        log.debug("[ws server]收到消息 {}",s);
        try {
            JSONObject jsonObject = JSONObject.parseObject(s);
            String echo = jsonObject.getString("echo");
            if (Strings.isNotBlank(echo)) {
                WsSyncRequestUtil.putEchoResult(echo,jsonObject);
                log.debug("echo消息：{}",echo);
                return;
            }
            Object obj = jsonObject.get("message");
            if(obj != null && obj instanceof String){
                log.error("message类型为string ： {}",obj);
                return;
            }
            final Message bean = JSONObject.parseObject(s, Message.class);
            if(PostTypeEnum.meta_event.toString().equals(bean.getPostType()) && MetaEventEnum.heartbeat.toString().equals(bean.getMetaEventType())){
                // 心跳包
                return;
            }
            ProcessMessageTask.execute(session,bean,s);
        }catch (Exception e){
            log.error("解析payload异常:{}",s,e);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("连接异常,sessionId:{}",session.getId(),exception);
        removeClient(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        log.info("连接断开,sessionId:{},{}",session.getId(),closeStatus.toString());
        removeClient(session);
    }

    public static void setBotIdToCache(WebSocketSession session, Long botId){
        UserSession userSession = sessionCache.get(session.getId());
        if(userSession != null){
            userSession.setBotId(botId);
            userSession.setSession(session);
        }else{
            sessionCache.put(session.getId(),new UserSession(botId,session));
        }
        
    }

    private void removeClient(WebSocketSession session){
        String id = session.getId();
        UserSession userSession = sessionCache.get(id);
        if(userSession != null){
            sessionCache.remove(id);
            log.info("客户端断开：{}  当前连接数：{}",userSession.getBotId(),getConnections());
        }
    }
    /**
     * 发送群消息
     * @param session 客户端session
     * @param groupId 群号
     * @param message 消息
     * @param autoEscape 是否以纯文本发送 true:以纯文本发送，不解析cq码
     */
    public static void sendGroupMessage(WebSocketSession session, Long groupId, String message, boolean autoEscape){
        RequestBox<Params> requestBox = new RequestBox<>();
        requestBox.setAction(GocqActionEnum.SEND_GROUP_MSG.getAction());

        Params params = new Params();
        params.setMessageType(MessageTypeEnum.group.getType());
        params.setAutoEscape(autoEscape);
        params.setGroupId(groupId);
        params.setMessage(message);

        requestBox.setParams(params);
        sendMessage(session,JSONObject.toJSONString(requestBox));
    }

    /**
     * 发送群聊合并消息
     * 自定义单条消息的uin和name
     * @param session
     * @param groupId
     * @param messages
     */
    public static void sendGroupMessage(WebSocketSession session, Long groupId,List<ForwardMsgItem> messages){
        if (!CollectionUtils.isEmpty(messages)) {
            RequestBox<Params> requestBox = new RequestBox<>();
            requestBox.setAction(GocqActionEnum.SEND_GROUP_FORWARD_MSG.getAction());
            Params params = new Params();
            params.setMessages(messages);
            params.setMessageType(MessageTypeEnum.group.getType());
            params.setGroupId(groupId);
            requestBox.setParams(params);
            sendMessage(session,JSONObject.toJSONString(requestBox));
        }
    }

    /**
     * 发送群合并消息
     * @param session 客户端session
     * @param groupId 群号
     * @param uin 合并卡片内的消息发送人qq
     * @param name 合并卡片内的消息发送人名称
     * @param messages 消息集合
     */
    public static void sendGroupMessage(WebSocketSession session, Long groupId,Long uin,String name, List<String> messages){
        if (!CollectionUtils.isEmpty(messages)) {
            RequestBox<Params> requestBox = createForwardMessageRequestBox(MessageTypeEnum.group,groupId,uin,name,messages);
            sendMessage(session,JSONObject.toJSONString(requestBox));
        }
    }

    /**
     * 发送群合并消息
     * 自动获取uin
     * @param session 客户端session
     * @param groupId 群号
     * @param name 合并卡片内的消息发送人名称
     * @param messages 消息集合
     */
    public static void sendGroupMessage(WebSocketSession session, Long groupId,String name, List<String> messages){
        if (!CollectionUtils.isEmpty(messages)) {
            Long uin = getUserBySession(session);
            RequestBox<Params> requestBox = createForwardMessageRequestBox(MessageTypeEnum.group,groupId,uin,name,messages);
            sendMessage(session,JSONObject.toJSONString(requestBox));
        }
    }

    /**
     * 发送群同步合并消息
     * @param session
     * @param groupId
     * @param uin
     * @param name
     * @param messages
     * @param timeout
     * @return
     */
    public static SyncResponse sendSyncGroupMessage(WebSocketSession session, Long groupId, Long uin, String name, List<String> messages, long timeout){
        Params params = createForwardMessageParams(MessageTypeEnum.group,groupId,uin,name,messages);
        JSONObject jsonObject = WsSyncRequestUtil.sendSyncRequest(session, GocqActionEnum.SEND_GROUP_FORWARD_MSG, params, timeout);
        if (jsonObject != null) {
            return JSONObject.parseObject(jsonObject.toJSONString(), SyncResponse.class);
        }
        return null;
    }


    /**
     * 发送私聊消息
     * @param session 客户端session
     * @param userId 对方qq
     * @param message 消息
     * @param autoEscape 是否以纯文本发送 true:以纯文本发送，不解析cq码
     */
    public static void sendPrivateMessage(WebSocketSession session, Long userId, String message, boolean autoEscape){
        RequestBox<Params> paramsRequestBox = new RequestBox<>();
        paramsRequestBox.setAction(GocqActionEnum.SEND_PRIVATE_MSG.getAction());

        Params params = new Params();
        params.setMessageType(MessageTypeEnum.privat.getType());
        params.setAutoEscape(autoEscape);
        params.setUserId(userId);
        params.setMessage(message);

        paramsRequestBox.setParams(params);

        sendMessage(session,JSONObject.toJSONString(paramsRequestBox));
    }

    /**
     * 发送私聊合并消息
     * 自定义单条消息的uin和name
     * @param session
     * @param userId
     * @param messages
     */
    public static void sendPrivateMessage(WebSocketSession session, Long userId,List<ForwardMsgItem> messages){
        if (!CollectionUtils.isEmpty(messages)) {
            RequestBox<Params> paramsRequestBox = new RequestBox<>();
            paramsRequestBox.setAction(GocqActionEnum.SEND_PRIVATE_FORWARD_MSG.getAction());
            Params params = new Params();
            params.setMessages(messages);
            params.setMessageType(MessageTypeEnum.privat.getType());
            params.setUserId(userId);
            paramsRequestBox.setParams(params);
            sendMessage(session,JSONObject.toJSONString(paramsRequestBox));
        }
    }

    /**
     * 发送私聊合并消息
     * @param session 客户端session
     * @param userId 对方qq
     * @param uin
     * @param name
     * @param messages
     */
    public static void sendPrivateMessage(WebSocketSession session, Long userId,Long uin,String name, List<String> messages){
        if (!CollectionUtils.isEmpty(messages)) {
            RequestBox<Params> paramsRequestBox = createForwardMessageRequestBox(MessageTypeEnum.privat,userId,uin,name,messages);
            sendMessage(session,JSONObject.toJSONString(paramsRequestBox));
        }
    }

    /**
     * 发送私聊合并消息
     * 自动获取uin
     * @param session 客户端session
     * @param userId 对方qq
     * @param name
     * @param messages
     */
    public static void sendPrivateMessage(WebSocketSession session, Long userId,String name, List<String> messages){
        if (!CollectionUtils.isEmpty(messages)) {
            Long uin = getUserBySession(session);
            RequestBox<Params> paramsRequestBox = createForwardMessageRequestBox(MessageTypeEnum.privat,userId,uin,name,messages);
            sendMessage(session,JSONObject.toJSONString(paramsRequestBox));
        }
    }

    /**
     * 发送私聊同步合并消息
     * @param session
     * @param userId
     * @param uin
     * @param name
     * @param messages
     * @param timeout
     * @return
     */
    public static SyncResponse sendSyncPrivateMessage(WebSocketSession session, Long userId, Long uin, String name, List<String> messages, long timeout){
        Params params = createForwardMessageParams(MessageTypeEnum.privat,userId,uin,name,messages);
        JSONObject jsonObject = WsSyncRequestUtil.sendSyncRequest(session, GocqActionEnum.SEND_PRIVATE_FORWARD_MSG, params, timeout);
        if (jsonObject != null) {
            return JSONObject.parseObject(jsonObject.toJSONString(), SyncResponse.class);
        }
        return null;
    }


    /**
     * 发送消息
     * 根据messageType来发送群还是私聊
     * @param session 客户端（gocq）session
     * @param userId 对方qq
     * @param groupId 群号
     * @param messageType private:发送私聊（userId生效）   group:发送群聊（groupId生效）
     * @param message 消息
     * @param autoEscape 是否以纯文本发送 true:以纯文本发送，不解析cq码
     */
    public static void sendMessage(WebSocketSession session, Long userId,Long groupId,String messageType, String message, boolean autoEscape){
        RequestBox<Params> paramsRequestBox = new RequestBox<>();
        paramsRequestBox.setAction(GocqActionEnum.SEND_MSG.getAction());

        Params params = new Params();
        params.setMessageType(messageType);
        params.setAutoEscape(autoEscape);
        params.setUserId(userId);
        params.setGroupId(groupId);
        params.setMessage(message);

        paramsRequestBox.setParams(params);

        sendMessage(session,JSONObject.toJSONString(paramsRequestBox));
    }

    /**
     * 合并发送消息
     * 根据messageType发送群还是私聊
     * @param session
     * @param userId
     * @param groupId
     * @param messageType
     * @param uin
     * @param name
     * @param messages
     */
    public static void sendMessage(WebSocketSession session, Long userId,Long groupId,String messageType,Long uin,String name, List<String> messages){
        RequestBox<Params> paramsRequestBox = new RequestBox<>();
        Params params = new Params();
        if (MessageTypeEnum.privat.getType().equals(messageType)) {
            paramsRequestBox.setAction(GocqActionEnum.SEND_PRIVATE_FORWARD_MSG.getAction());
            params.setUserId(userId);
        }else if (MessageTypeEnum.group.getType().equals(messageType)) {
            paramsRequestBox.setAction(GocqActionEnum.SEND_GROUP_FORWARD_MSG.getAction());
            params.setGroupId(groupId);
        }
        params.setMessageType(messageType);
        List<ForwardMsgItem> forwardMsgs = new ArrayList<>(messages.size());
        for (String s : messages) {
            forwardMsgs.add(createForwardMsgItem(uin,name,s));
        }
        params.setMessages(forwardMsgs);
        paramsRequestBox.setParams(params);

        sendMessage(session,JSONObject.toJSONString(paramsRequestBox));
    }

    /**
     * 合并同步发送消息
     * 根据messageType发送群还是私聊
     * @param session
     * @param userId
     * @param groupId
     * @param messageType
     * @param uin
     * @param name
     * @param messages
     * @param timeout
     * @return
     */
    public static SyncResponse sendSyncMessage(WebSocketSession session, Long userId,Long groupId,String messageType,Long uin,String name, List<String> messages,long timeout){
        Params params = new Params();
        GocqActionEnum actionEnum = null;
        if (MessageTypeEnum.privat.getType().equals(messageType)) {
            actionEnum = GocqActionEnum.SEND_PRIVATE_FORWARD_MSG;
            params.setUserId(userId);
        }else if (MessageTypeEnum.group.getType().equals(messageType)) {
            actionEnum = GocqActionEnum.SEND_GROUP_FORWARD_MSG;
            params.setGroupId(groupId);
        }
        params.setMessageType(messageType);

        List<ForwardMsgItem> forwardMsgs = new ArrayList<>(messages.size());
        for (String s : messages) {
            forwardMsgs.add(createForwardMsgItem(uin,name,s));
        }
        params.setMessages(forwardMsgs);
        JSONObject jsonObject = WsSyncRequestUtil.sendSyncRequest(session,actionEnum, params, timeout);
        if (jsonObject != null) {
            return JSONObject.parseObject(jsonObject.toJSONString(), SyncResponse.class);
        }
        return null;
    }

    public static void sendMessage(WebSocketSession session, String text){
        try {
            log.debug("发送消息给gocq {}",text);
            session.sendMessage(new TextMessage(text));
        } catch (Exception e) {
            log.error("发送消息发生异常,session:{},消息：{}",session,text,e);
        }
    }

    public static Long getUserBySession(WebSocketSession session){
        UserSession userSession = sessionCache.get(session.getId());
        
        if(userSession != null){
            return userSession.getBotId() != null ? userSession.getBotId() : BotConfig.DEFAULT_USER;
        }
        return BotConfig.DEFAULT_USER;
    }

    public static WebSocketSession getSessionByBot(Long botId){
        for (Map.Entry<String, UserSession> entry : sessionCache.entrySet()) {
            if (entry.getValue() != null 
                    && entry.getValue().getBotId() != null
                     && entry.getValue().getBotId().equals(botId)) {
                return entry.getValue().getSession();
            }
        }
        return null;
    }


    private static RequestBox<Params> createForwardMessageRequestBox(MessageTypeEnum messageType, Long id, Long uin, String name, List<String> messages){
        RequestBox<Params> paramsRequestBox = new RequestBox<>();
        Params params = new Params();
        List<ForwardMsgItem> forwardMsgs = new ArrayList<>(messages.size());
        for (String message : messages) {
            forwardMsgs.add(createForwardMsgItem(uin,name,message));
        }
        params.setMessages(forwardMsgs);
        params.setMessageType(messageType.getType());
        if (MessageTypeEnum.group.getType().equals(messageType.getType())) {
            paramsRequestBox.setAction(GocqActionEnum.SEND_GROUP_FORWARD_MSG.getAction());
            params.setGroupId(id);
        }else if(MessageTypeEnum.privat.getType().equals(messageType.getType())){
            paramsRequestBox.setAction(GocqActionEnum.SEND_PRIVATE_FORWARD_MSG.getAction());
            params.setUserId(id);
        }
        paramsRequestBox.setParams(params);
        return paramsRequestBox;
    }

    private static Params createForwardMessageParams(MessageTypeEnum messageType, Long id, Long uin, String name, List<String> messages){
        Params params = new Params();
        if (MessageTypeEnum.privat.getType().equals(messageType.getType())) {
            params.setUserId(id);
        }else if(MessageTypeEnum.group.getType().equals(messageType.getType())){
            params.setGroupId(id);
        }
        params.setMessageType(messageType.getType());
        List<ForwardMsgItem> forwardMsgs = new ArrayList<>(messages.size());
        for (String s : messages) {
            forwardMsgs.add(createForwardMsgItem(uin,name,s));
        }
        params.setMessages(forwardMsgs);
        return params;
    }

    private static ForwardMsgItem createForwardMsgItem(Long uin, String name, String context){
        ForwardMsgItem item = new ForwardMsgItem();
        ForwardMsgItem.Data data = new ForwardMsgItem.Data();
        data.setUin(uin);
        data.setName(name);
        data.setContent(context);
        item.setData(data);
        return item;
    }

    public static void main(String[] args) {
        String s = "{\"status\":\"ok\",\"retcode\":0,\"data\":{\"message_id\":-2147482581},\"message\":\"\",\"wording\":\"\"}";
        try {
            JSONObject jsonObject = JSONObject.parseObject(s);
            if(jsonObject.get("message") instanceof String){
                System.out.println("111");
            }
            JSONObject.parseObject(s,Message.class);
            System.out.println();
        }catch (Exception e){
            e.printStackTrace();
        }
        
    }

}
