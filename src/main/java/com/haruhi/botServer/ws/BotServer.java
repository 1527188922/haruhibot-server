package com.haruhi.botServer.ws;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
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

import java.util.Arrays;

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
