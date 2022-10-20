package com.haruhi.botServer.ws;

import com.alibaba.fastjson.JSONObject;
import com.haruhi.botServer.constant.GocqActionEnum;
import com.haruhi.botServer.constant.event.MessageEventEnum;
import com.haruhi.botServer.constant.event.MetaEventEnum;
import com.haruhi.botServer.constant.event.PostTypeEnum;
import com.haruhi.botServer.constant.event.SubTypeEnum;
import com.haruhi.botServer.dispenser.MessageDispenser;
import com.haruhi.botServer.dispenser.NoticeDispenser;
import com.haruhi.botServer.dto.gocq.request.ForwardMsg;
import com.haruhi.botServer.dto.gocq.request.Params;
import com.haruhi.botServer.dto.gocq.request.RequestBox;
import com.haruhi.botServer.dto.gocq.response.Message;
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
 * gocq使用反向ws连接
 */
@Slf4j
public class ServerEndpoint implements WebSocketHandler {

    private static Map<String,WebSocketSession> sessionMap = new ConcurrentHashMap<>();
    private static Map<String,Long> userIdMap = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessionMap.put(session.getId(),session);
        log.info("客户端连接成功,sessionId:{}，客户端数量：{}", session.getId(),sessionMap.size());
    }

    @Override
    public void handleMessage(WebSocketSession session, final WebSocketMessage<?> message) throws Exception {
        final String s = String.valueOf(message.getPayload());
        log.info("handler收到消息,session:{},message:{}",session.getId(),s);
        Message bean = JSONObject.parseObject(s, Message.class);
        if(PostTypeEnum.meta_event.toString().equals(bean.getPost_type()) && MetaEventEnum.heartbeat.toString().equals(bean.getMeta_event_type())){
            // 心跳包
            return;
        }

        if(PostTypeEnum.message.toString().equals(bean.getPost_type())){
            // 普通消息
            final String command = bean.getMessage();
            log.info("[{}]收到来自用户[{}]的消息:{}",bean.getMessage_type(),bean.getUser_id(),command);
            if(command != null){
                MessageDispenser.onEvent(session,bean,command);
            }
        }else if(PostTypeEnum.notice.toString().equals(bean.getPost_type())){
            // bot通知
            NoticeDispenser.onEvent(session,bean);
        } else if(PostTypeEnum.meta_event.toString().equals(bean.getPost_type())){
            // 系统消息
            if(MetaEventEnum.lifecycle.toString().equals(bean.getMeta_event_type()) && SubTypeEnum.connect.toString().equals(bean.getSub_type())){
                // 刚连接成功时，gocq会发一条消息给bot
                userIdMap.put(session.getId(),bean.getSelf_id());
            }
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("异常:session:{}",session.getId(),exception);
        removeClient(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        log.info("连接断开:session:{}",session.getId());
        removeClient(session);
    }

    private void removeClient(WebSocketSession session){
        String id = session.getId();
        sessionMap.remove(id);
        Long userId = userIdMap.get(id);
        if (userId != null) {
            userIdMap.remove(id);
        }
        log.info("用户：{}断开，客户端数量：{}",userId,sessionMap.size());
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    /**
     * 发送群消息
     * @param session 客户端（gocq）session
     * @param groupId 群号
     * @param message 消息
     * @param autoEscape 是否以纯文本发送 true:以纯文本发送，不解析cq码
     */
    public static void sendGroupMessage(WebSocketSession session, Long groupId, String message, boolean autoEscape){
        RequestBox<Params> paramsRequestBox = new RequestBox<>();
        paramsRequestBox.setAction(GocqActionEnum.SEND_GROUP_MSG.getAction());

        Params params = new Params();
        params.setMessage_type(MessageEventEnum.group.getType());
        params.setAuto_escape(autoEscape);
        params.setGroup_id(groupId);
        params.setMessage(message);

        paramsRequestBox.setParams(params);
        sendMessage(session,JSONObject.toJSONString(paramsRequestBox));
    }


    /**
     * 发送合并消息 （只能发群，默认解析cq码）
     * @param session 客户端（gocq）session
     * @param groupId 群号
     * @param uin 合并卡片内的消息发送人qq
     * @param name 合并卡片内的消息发送人名称
     * @param messages 消息集合
     */
    public static void sendGroupMessage(WebSocketSession session, Long groupId,Long uin,String name, List<String> messages){
        if (!CollectionUtils.isEmpty(messages)) {
            RequestBox<Params> paramsRequestBox = new RequestBox<>();
            paramsRequestBox.setAction(GocqActionEnum.SEND_GROUP_FORWARD_MSG.getAction());
            Params params = new Params();
            params.setMessage_type(MessageEventEnum.group.getType());
            params.setGroup_id(groupId);
            ArrayList<ForwardMsg> forwardMsgs = new ArrayList<>(messages.size());
            for (String s : messages) {
                ForwardMsg item = new ForwardMsg();
                ForwardMsg.Data data = new ForwardMsg.Data();
                data.setUin(uin);
                data.setName(name);
                data.setContent(s);
                item.setData(data);
                forwardMsgs.add(item);
            }

            params.setMessages(forwardMsgs);
            paramsRequestBox.setParams(params);
            sendMessage(session,JSONObject.toJSONString(paramsRequestBox));
        }
    }

    /**
     * 发送私聊消息
     * @param session 客户端（gocq）session
     * @param userId 对方qq
     * @param message 消息
     * @param autoEscape 是否以纯文本发送 true:以纯文本发送，不解析cq码
     */
    public static void sendPrivateMessage(WebSocketSession session, Long userId, String message, boolean autoEscape){
        RequestBox<Params> paramsRequestBox = new RequestBox<>();
        paramsRequestBox.setAction(GocqActionEnum.SEND_PRIVATE_MSG.getAction());

        Params params = new Params();
        params.setMessage_type(MessageEventEnum.privat.getType());
        params.setAuto_escape(autoEscape);
        params.setUser_id(userId);
        params.setMessage(message);

        paramsRequestBox.setParams(params);

        sendMessage(session,JSONObject.toJSONString(paramsRequestBox));
    }


    /**
     * 发送消息
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


    private static void sendMessage(WebSocketSession session, String text){
        try {
            session.sendMessage(new TextMessage(text));
        } catch (Exception e) {
            log.error("发送消息发生异常,消息：{}",text,e);
        }
    }

}
