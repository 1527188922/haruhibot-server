package com.haruhi.botServer.handlers.message;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Role;
import com.haruhi.botServer.constant.DictionaryEnum;
import com.haruhi.botServer.constant.HandlerWeightEnum;
import com.haruhi.botServer.dto.qqclient.Message;
import com.haruhi.botServer.event.message.IAllMessageEvent;
import com.haruhi.botServer.service.DictionarySqliteService;
import com.haruhi.botServer.utils.ApplicationContextProvider;
import com.haruhi.botServer.utils.MatchResult;
import com.haruhi.botServer.utils.ThreadPoolUtil;
import com.haruhi.botServer.ws.Bot;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
public class QanWenHandler implements IAllMessageEvent {

    private static final ConcurrentHashMap<String, QianWen> CACHE = new ConcurrentHashMap<>();
    private static final String MODEL = "qwen1.5-72b-chat";

    private String key(Message message){
        if (message.isGroupMsg()) {
            return String.valueOf(message.getSelfId()) + message.getGroupId();
        }
        return String.valueOf(message.getSelfId()) + message.getUserId();
    }


    @Override
    public String funName() {
        return HandlerWeightEnum.W_880.getName();
    }
    
    @Override
    public int weight() {
        return HandlerWeightEnum.W_880.getWeight();
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
            QianWen qianWen = CACHE.get(key(message));
            if(qianWen == null){
                qianWen = new QianWen(MODEL);
                CACHE.put(key,qianWen);
            }
            String res = qianWen.call(data);
            bot.sendMessage(message.getUserId(),message.getGroupId(),message.getMessageType(),res,true);
        });
        return true;
    }
    private MatchResult<String> matches(Message message){
        if(!message.isTextMsg()){
            return MatchResult.unmatched();
        }
        String prefix = "qw ";
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
    
    
    public static class QianWen{
        private final String model;
        private final AtomicBoolean lock;
        private final List<com.alibaba.dashscope.common.Message> history;
        private final Generation gen;
        
        public QianWen(String model){
            this.model = model;
            lock = new AtomicBoolean(false);
            history = new ArrayList<>();
            gen = new Generation();
        }
        
        public void clear(){
            history.clear();
        }
        
        public String call(String msg){
            if (!lock.compareAndSet(false,true)) {
                return "上一任务还未结束，请稍等。。。";
            }
            try {
                history.add(createMessage(Role.USER,msg));
                GenerationParam generationParam = createGenerationParam(history);
                GenerationResult call = gen.call(generationParam);
                com.alibaba.dashscope.common.Message message = call.getOutput().getChoices().getFirst().getMessage();
                String content = message.getContent();
                history.add(message);
                return content;
            }catch (Exception e){
                history.remove(history.size() - 1);
                log.error("请求千问异常",e);
                return "异常：" + e.getMessage();
            }finally {
                lock.set(false);
            }
            
        }

        private GenerationParam createGenerationParam(List<com.alibaba.dashscope.common.Message> messages) {

            String apiKey = ApplicationContextProvider.getBean(DictionarySqliteService.class)
                    .getInCache(DictionaryEnum.QIANWEN_API_KEY.getKey(), null);

            return GenerationParam.builder()
                    .model(model)
                    .messages(messages)
                    .apiKey(apiKey)
                    .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                    .topP(0.8)
                    .build();
        }
        private com.alibaba.dashscope.common.Message createMessage(Role role, String content) {
            return com.alibaba.dashscope.common.Message.builder().role(role.getValue()).content(content).build();
        }
        
    }

   
}
