package com.haruhi.botServer.ws;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.fastjson.JSONObject;
import com.haruhi.botServer.config.BotConfig;
import com.haruhi.botServer.constant.GocqActionEnum;
import com.haruhi.botServer.constant.event.MessageTypeEnum;
import com.haruhi.botServer.dto.gocq.request.ForwardMsgItem;
import com.haruhi.botServer.dto.gocq.request.Params;
import com.haruhi.botServer.dto.gocq.request.RequestBox;
import com.haruhi.botServer.dto.gocq.response.SyncResponse;
import com.haruhi.botServer.dto.gocq.response.Message;
import com.haruhi.botServer.thread.MessageProcessor;
import com.haruhi.botServer.utils.WsSyncRequestUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 机器人是服务端
 * gocq（客户端）使用反向ws连接
 */
@Slf4j
@Component
public class Server extends TextWebSocketHandler {

    @Autowired
    private MessageProcessor messageProcessor;

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
    public static WebSocketSession getSession(){
        if (getConnections() > 0) {
            return ((UserSession)sessionCache.values().toArray()[0]).getSession();
        }
        return null;
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
                log.info("echo响应：{}",s);
                return;
            }
            if(jsonObject.containsKey("retcode") && jsonObject.containsKey("status")){
                // {"status":"ok","retcode":0,"data":{"message_id":-2147139720},"message":"","wording":""}
                log.info("非echo响应：{}",s);
                return;
            }
            // {"time":1731649946,"self_id":2995339277,"post_type":"meta_event","meta_event_type":"lifecycle","sub_type":"connect"}
            // {"time":1731650096,"self_id":2995339277,"post_type":"meta_event","meta_event_type":"heartbeat","status":{"online":true,"good":true},"interval":30000}
            final Message bean = JSONObject.parseObject(s, Message.class);
            messageProcessor.execute(session, bean);
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
     * @param autoEscape 是否以纯文本发送 true:以纯文本发送，不解析cq码 false:解析cq码
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

    public static void main(String[] args) throws NoApiKeyException, InputRequiredException {
//        ChatResponse response = new Qianfan("7bf5c7f06a624a0ebb240a38931aad00","0a3b12a2398949fc911f479942913528").chatCompletion()
//                .model("ERNIE-Lite-8K-0922") // 使用model指定预置模型
//                // .endpoint("completions_pro") // 也可以使用endpoint指定任意模型 (二选一)
//                .addMessage("user", "你好") // 添加用户消息 (此方法可以调用多次，以实现多轮对话的消息传递)
////                .temperature(0.7) // 自定义超参数
//                .execute(); // 发起请求
//        System.out.println(response.getResult());
//        System.out.println();

        Generation gen = new Generation();
        com.alibaba.dashscope.common.Message userMsg = com.alibaba.dashscope.common.Message.builder()
                .role(Role.USER.getValue()).content("如何评价网络热梗").build();
        GenerationParam param =
                GenerationParam.builder().model("qwen2-1.5b-instruct").messages(Arrays.asList(userMsg))
                        .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                        .apiKey("")
                        .topP(0.8)
                        .build();
        GenerationResult result = gen.call(param);
        System.out.println(result);
    }

}
