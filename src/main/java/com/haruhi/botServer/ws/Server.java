package com.haruhi.botServer.ws;

import com.alibaba.fastjson.JSONObject;
import com.haruhi.botServer.config.BotConfig;
import com.haruhi.botServer.constant.GocqActionEnum;
import com.haruhi.botServer.constant.event.MessageTypeEnum;
import com.haruhi.botServer.constant.event.MetaEventEnum;
import com.haruhi.botServer.constant.event.PostTypeEnum;
import com.haruhi.botServer.dto.gocq.request.ForwardMsg;
import com.haruhi.botServer.dto.gocq.request.Params;
import com.haruhi.botServer.dto.gocq.request.RequestBox;
import com.haruhi.botServer.dto.gocq.response.Message;
import com.haruhi.botServer.dto.gocq.response.SyncResponse;
import com.haruhi.botServer.thread.ProcessMessageTask;
import com.haruhi.botServer.utils.GocqSyncRequestUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 机器人是服务端
 * gocq（客户端）使用反向ws连接
 */
@Slf4j
public class Server implements WebSocketHandler {

    private static Map<String,WebSocketSession> sessionMap = new ConcurrentHashMap<>();
    private static Map<String,Long> userIdMap = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessionMap.put(session.getId(),session);
        log.info("客户端连接成功,sessionId:{}，客户端数量：{}", session.getId(),sessionMap.size());
    }

    @Override
    public void handleMessage(WebSocketSession session, final WebSocketMessage<?> message) throws Exception {
        Object payload = message.getPayload();
        try {
            final String s = String.valueOf(payload);
            Message bean = JSONObject.parseObject(s, Message.class);
            if(PostTypeEnum.meta_event.toString().equals(bean.getPost_type()) && MetaEventEnum.heartbeat.toString().equals(bean.getMeta_event_type())){
                // 心跳包
                return;
            }
            ProcessMessageTask.execute(session,bean,s);
        }catch (Exception e){
            log.error("解析payload异常:{}",payload);
            throw e;
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

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    public static void putUserIdMap(String key,Long val){
        userIdMap.put(key,val);
    }

    private void removeClient(WebSocketSession session){
        String id = session.getId();
        Long userId = userIdMap.get(id);
        if (userId != null) {
            userIdMap.remove(id);
            log.info("用户断开：{}",userId);
        }
        if (sessionMap.containsKey(id)) {
            sessionMap.remove(id);
            log.info("客户端数量：{}",sessionMap.size());
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
        RequestBox<Params> paramsRequestBox = new RequestBox<>();
        paramsRequestBox.setAction(GocqActionEnum.SEND_GROUP_MSG.getAction());

        Params params = new Params();
        params.setMessage_type(MessageTypeEnum.group.getType());
        params.setAuto_escape(autoEscape);
        params.setGroup_id(groupId);
        params.setMessage(message);

        paramsRequestBox.setParams(params);
        sendMessage(session,JSONObject.toJSONString(paramsRequestBox));
    }

    /**
     * 发送群聊合并消息
     * 自定义单条消息的uin和name
     * @param session
     * @param groupId
     * @param messages
     */
    public static void sendGroupMessage(WebSocketSession session, Long groupId,List<ForwardMsg> messages){
        if (!CollectionUtils.isEmpty(messages)) {
            RequestBox<Params> paramsRequestBox = new RequestBox<>();
            paramsRequestBox.setAction(GocqActionEnum.SEND_GROUP_FORWARD_MSG.getAction());
            Params params = new Params();
            params.setMessages(messages);
            params.setMessage_type(MessageTypeEnum.group.getType());
            params.setGroup_id(groupId);
            paramsRequestBox.setParams(params);
            sendMessage(session,JSONObject.toJSONString(paramsRequestBox));
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
            RequestBox<Params> paramsRequestBox = createForwardMessageRequestBox(MessageTypeEnum.group,groupId,uin,name,messages);
            sendMessage(session,JSONObject.toJSONString(paramsRequestBox));
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
            RequestBox<Params> paramsRequestBox = createForwardMessageRequestBox(MessageTypeEnum.group,groupId,uin,name,messages);
            sendMessage(session,JSONObject.toJSONString(paramsRequestBox));
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
        JSONObject jsonObject = GocqSyncRequestUtil.sendSyncRequest(session, GocqActionEnum.SEND_GROUP_FORWARD_MSG, params, timeout);
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
        params.setMessage_type(MessageTypeEnum.privat.getType());
        params.setAuto_escape(autoEscape);
        params.setUser_id(userId);
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
    public static void sendPrivateMessage(WebSocketSession session, Long userId,List<ForwardMsg> messages){
        if (!CollectionUtils.isEmpty(messages)) {
            RequestBox<Params> paramsRequestBox = new RequestBox<>();
            paramsRequestBox.setAction(GocqActionEnum.SEND_PRIVATE_FORWARD_MSG.getAction());
            Params params = new Params();
            params.setMessages(messages);
            params.setMessage_type(MessageTypeEnum.privat.getType());
            params.setUser_id(userId);
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
        JSONObject jsonObject = GocqSyncRequestUtil.sendSyncRequest(session, GocqActionEnum.SEND_PRIVATE_FORWARD_MSG, params, timeout);
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
        params.setMessage_type(messageType);
        params.setAuto_escape(autoEscape);
        params.setUser_id(userId);
        params.setGroup_id(groupId);
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
            params.setUser_id(userId);
        }else if (MessageTypeEnum.group.getType().equals(messageType)) {
            paramsRequestBox.setAction(GocqActionEnum.SEND_GROUP_FORWARD_MSG.getAction());
            params.setGroup_id(groupId);
        }
        params.setMessage_type(messageType);
        List<ForwardMsg> forwardMsgs = new ArrayList<>(messages.size());
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
            params.setUser_id(userId);
        }else if (MessageTypeEnum.group.getType().equals(messageType)) {
            actionEnum = GocqActionEnum.SEND_GROUP_FORWARD_MSG;
            params.setGroup_id(groupId);
        }
        params.setMessage_type(messageType);

        List<ForwardMsg> forwardMsgs = new ArrayList<>(messages.size());
        for (String s : messages) {
            forwardMsgs.add(createForwardMsgItem(uin,name,s));
        }
        params.setMessages(forwardMsgs);
        JSONObject jsonObject = GocqSyncRequestUtil.sendSyncRequest(session,actionEnum, params, timeout);
        if (jsonObject != null) {
            return JSONObject.parseObject(jsonObject.toJSONString(), SyncResponse.class);
        }
        return null;
    }

    public static void sendMessage(WebSocketSession session, String text){
        try {
            session.sendMessage(new TextMessage(text));
        } catch (Exception e) {
            log.error("发送消息发生异常,session:{},消息：{}",session,text,e);
            throw new RuntimeException("Sending message exception");
        }
    }

    public static Long getUserBySession(WebSocketSession session){
        Long userId = userIdMap.get(session.getId());
        if(userId != null){
            return userId;
        }
        return BotConfig.DEFAULT_USER;
    }

    private static RequestBox<Params> createForwardMessageRequestBox(MessageTypeEnum messageType, Long id, Long uin, String name, List<String> messages){
        RequestBox<Params> paramsRequestBox = new RequestBox<>();
        Params params = new Params();
        List<ForwardMsg> forwardMsgs = new ArrayList<>(messages.size());
        for (String message : messages) {
            forwardMsgs.add(createForwardMsgItem(uin,name,message));
        }
        params.setMessages(forwardMsgs);
        params.setMessage_type(messageType.getType());
        if (MessageTypeEnum.group.getType().equals(messageType.getType())) {
            paramsRequestBox.setAction(GocqActionEnum.SEND_GROUP_FORWARD_MSG.getAction());
            params.setGroup_id(id);
        }else if(MessageTypeEnum.privat.getType().equals(messageType.getType())){
            paramsRequestBox.setAction(GocqActionEnum.SEND_PRIVATE_FORWARD_MSG.getAction());
            params.setUser_id(id);
        }
        paramsRequestBox.setParams(params);
        return paramsRequestBox;
    }

    private static Params createForwardMessageParams(MessageTypeEnum messageType, Long id, Long uin, String name, List<String> messages){
        Params params = new Params();
        if (MessageTypeEnum.privat.getType().equals(messageType.getType())) {
            params.setUser_id(id);
        }else if(MessageTypeEnum.group.getType().equals(messageType.getType())){
            params.setGroup_id(id);
        }
        params.setMessage_type(messageType.getType());
        List<ForwardMsg> forwardMsgs = new ArrayList<>(messages.size());
        for (String s : messages) {
            forwardMsgs.add(createForwardMsgItem(uin,name,s));
        }
        params.setMessages(forwardMsgs);
        return params;
    }

    private static ForwardMsg createForwardMsgItem(Long uin, String name, String context){
        ForwardMsg item = new ForwardMsg();
        ForwardMsg.Data data = new ForwardMsg.Data();
        data.setUin(uin);
        data.setName(name);
        data.setContent(context);
        item.setData(data);
        return item;
    }

}
