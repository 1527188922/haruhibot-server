package com.haruhi.botServer.handlers.message;

import com.github.plexpt.chatgpt.Chatbot;
import com.haruhi.botServer.cache.CacheMap;
import com.haruhi.botServer.config.ChatgptConfig;
import com.haruhi.botServer.constant.RegexEnum;
import com.haruhi.botServer.dto.gocq.response.Message;
import com.haruhi.botServer.event.message.IMessageEvent;
import com.haruhi.botServer.utils.CommonUtil;
import com.haruhi.botServer.utils.ThreadPoolUtil;
import com.haruhi.botServer.ws.Server;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
public class ChatpgtHandler implements IMessageEvent {

    @Override
    public int weight() {
        return 70;
    }

    @Override
    public String funName() {
        return "ChatGPT";
    }

    private static final CacheMap<String,StateChatbot> CHATBOT_CACHE = new CacheMap<>(20, TimeUnit.HOURS,50);

    private String key(Long selfId,Long userId){
        return String.valueOf(selfId) + userId;
    }

    @Override
    public boolean onMessage(final WebSocketSession session,final Message message,final String command) {
        String text = CommonUtil.commandReplaceFirst(command,RegexEnum.CHATGPT);
        if (Strings.isBlank(text)) {
            return false;
        }

        if(!ChatgptConfig.support()){
            Server.sendMessage(session,message.getUserId(),message.getGroupId(),message.getMessageType(),
                    "请联系bot管理员qq配置ChatGPT session-token 或 邮箱和密码！",true);
            return true;
        }
        // 先暂时只处理有session-token的情况

        String key = key(message.getSelfId(), message.getUserId());
        StateChatbot item = CHATBOT_CACHE.get(key);
        if(item == null){
            StateChatbot chatbot = new StateChatbot(ChatgptConfig.SESSION_TOKEN);
            CHATBOT_CACHE.put(key,chatbot);
            item = chatbot;
        }else{
            if(item.getSending()){
                Server.sendMessage(session,message.getUserId(),message.getGroupId(),message.getMessageType(),
                        "您的ChatGPT正在处理中。。",true);

              return true;
            }
        }
        ThreadPoolUtil.getHandleCommandPool().execute(new Task(session,message,text,item));
        return true;
    }


    private class Task implements Runnable{

        private WebSocketSession session;
        private Message message;
        private StateChatbot chatbot;
        private String text;

        public Task(WebSocketSession session,Message message,String text,StateChatbot chatbot){
            this.session = session;
            this.message = message;
            this.text = text;
            chatbot.setSending(true);
            this.chatbot = chatbot;
        }

        @Override
        public void run() {
            try {
                Map<String, Object> chatResponse = chatbot.getChatResponse(text);
                Object reply = chatResponse.get("message");
                if(reply instanceof String){
                    Server.sendMessage(session,message.getUserId(),message.getGroupId(),message.getMessageType(),String.valueOf(reply),true);
                }else {
                    log.error("reply非string类型,reply：{}",reply);
                }
            }catch (Exception e){
                log.error("获取chatgpt异常",e);
            }finally {
                chatbot.setSending(false);
            }
        }
    }


}

class StateChatbot extends Chatbot{

    private AtomicBoolean sending;

    public StateChatbot(String sessionToken) {
        super(sessionToken);
        this.sending = new AtomicBoolean(false);
    }

    public boolean getSending(){
        return sending.get();
    }

    public void setSending(boolean sending){
        this.sending.set(sending);
    }

}
