package com.haruhi.botServer.handlers.message;

import com.haruhi.botServer.constant.HandlerWeightEnum;
import com.haruhi.botServer.dto.qqclient.Message;
import com.haruhi.botServer.event.message.IAllMessageEvent;
import com.haruhi.botServer.utils.MatchResult;
import com.haruhi.botServer.utils.OpenAiServiceHolder;
import com.haruhi.botServer.utils.ThreadPoolUtil;
import com.haruhi.botServer.ws.Bot;
import com.theokanning.openai.completion.chat.ChatCompletionChoice;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@Slf4j
public class DeepSeekHandler implements IAllMessageEvent {

    private static final ConcurrentHashMap<String, DeepSeek> CACHE = new ConcurrentHashMap<>();

    private String key(Message message){
        if (message.isGroupMsg()) {
            return String.valueOf(message.getSelfId()) + message.getGroupId();
        }
        return String.valueOf(message.getSelfId()) + message.getUserId();
    }

    @Override
    public String funName() {
        return HandlerWeightEnum.W_870.getName();
    }

    @Override
    public int weight() {
        return HandlerWeightEnum.W_870.getWeight();
    }


    @Override
    public boolean onMessage(Bot bot, Message message) {
        MatchResult<String> match = matches(message);
        if(!match.isMatched()){
            return false;
        }

        ThreadPoolUtil.getHandleCommandPool().execute(()->{
            String data = match.getData();
            String key = key(message);
            DeepSeek deepSeek = CACHE.get(key(message));
            if(deepSeek == null){
                deepSeek = new DeepSeek("deepseek-chat");
                CACHE.put(key, deepSeek);
            }
            String res = deepSeek.call(data);
            bot.sendMessage(message.getUserId(),message.getGroupId(),message.getMessageType(),res,true);
        });
        return true;
    }
    private MatchResult<String> matches(Message message){
        if(!message.isTextMsg()){
            return MatchResult.unmatched();
        }
        String prefix = "ds ";
        String text = message.getText(0);
        if(!text.startsWith(prefix)){
            return MatchResult.unmatched();
        }
        String msg = text.replaceFirst(prefix,"");
        if(StringUtils.isBlank(msg)){
            return MatchResult.unmatched();
        }
        return MatchResult.matched(msg);
    }



    @AllArgsConstructor
    public static class DeepSeek{
        private final String model;
        private final AtomicBoolean lock;
        private final List<ChatMessage> history;

        public DeepSeek(String model){
            this.model = model;
            lock = new AtomicBoolean(false);
            history = new ArrayList<>();
        }

        public void clear(){
            history.clear();
        }

        public String call(String msg){
            if (!lock.compareAndSet(false,true)) {
                return "上一任务还未结束，请稍等。。。";
            }
            try {
                history.add(new ChatMessage(ChatMessageRole.USER.value(), msg));
                ChatCompletionRequest request = createRequest(history);
                ChatCompletionResult chatCompletion =  OpenAiServiceHolder.getOpenAiService().createChatCompletion(request);
                List<ChatCompletionChoice> choices = chatCompletion.getChoices();
                ChatMessage resMessage = choices.getFirst().getMessage();
                String content = resMessage.getContent();
                history.add(resMessage);
                return content;
            }catch (Exception e){
                history.removeLast();
                log.error("请求deepseek异常",e);
                return "异常：" + e.getMessage();
            }finally {
                lock.set(false);
            }

        }

        private ChatCompletionRequest createRequest(List<ChatMessage> messages) {
            ChatCompletionRequest request = ChatCompletionRequest.builder()
                    .model(this.model) // 模型名，可选gpt-4/gpt-4o等
                    .messages(messages)
//                .temperature(0.7) // 创造力，0-2，值越大越随机
//                .maxTokens(1024) // 最大返回令牌数
                    .build();

            return request;
        }

    }


}
