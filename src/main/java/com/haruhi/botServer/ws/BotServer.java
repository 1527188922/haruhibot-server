package com.haruhi.botServer.ws;

import com.alibaba.fastjson.JSONObject;
import com.haruhi.botServer.dto.qqclient.Message;
import com.haruhi.botServer.thread.MessageProcessor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * 机器人是服务端
 * gocq（客户端）使用反向ws连接
 */
@Slf4j
@Component
public class BotServer extends TextWebSocketHandler {

    @Autowired
    private MessageProcessor messageProcessor;

    // 逻辑启停 实际上服务并没有停止 只是拒绝连接(握手拦截器中去拒绝)
    private volatile boolean isRunning = true;

    public boolean isRunning() {
        return isRunning;
    }
    public void stop(){
        isRunning = false;
        BotContainer.removeAllClient();
    }
    public void start(){
        isRunning = true;
    }

    @Override
    public void afterConnectionEstablished(final WebSocketSession session) throws Exception {
        BotContainer.add(null,session);
        log.info("客户端连接成功,sessionId:{}，客户端数量：{}", session.getId(),BotContainer.getConnections());
    }

    @Override
    public void handleTextMessage(final WebSocketSession session, final TextMessage message) throws Exception {
        final String s = message.getPayload();
        log.debug("[ws server]收到消息 {}",s);
        try {
            Bot bot = BotContainer.getBotBySession(session);

            JSONObject jsonObject = JSONObject.parseObject(s);
            String echo = jsonObject.getString("echo");
            if (Strings.isNotBlank(echo)) {
                bot.putEchoResult(echo,jsonObject);
                log.info("echo响应：{}",s);
                return;
            }
            if(jsonObject.containsKey("retcode") && jsonObject.containsKey("status")){
                log.info("非echo响应：{}",s);
                return;
            }
            final Message bean = JSONObject.parseObject(s, Message.class);
            bean.setRawWsMsg(s);
            messageProcessor.execute(bot, bean);
        }catch (Exception e){
            log.error("解析payload异常:{}",s,e);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("连接异常,sessionId:{}",session.getId(),exception);
        BotContainer.removeClient(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        log.info("连接断开,sessionId:{},{}",session.getId(),closeStatus.toString());
        BotContainer.removeClient(session);
    }

}
