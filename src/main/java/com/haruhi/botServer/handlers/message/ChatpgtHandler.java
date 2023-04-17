//package com.haruhi.botServer.handlers.message;
//
//import com.github.plexpt.chatgpt.Chatbot;
//import com.haruhi.botServer.config.ChatgptConfig;
//import com.haruhi.botServer.constant.RegexEnum;
//import com.haruhi.botServer.constant.event.MessageTypeEnum;
//import com.haruhi.botServer.dto.gocq.response.Message;
//import com.haruhi.botServer.event.message.IAllMessageEvent;
//import com.haruhi.botServer.utils.CommonUtil;
//import com.haruhi.botServer.utils.ThreadPoolUtil;
//import com.haruhi.botServer.ws.Server;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.logging.log4j.util.Strings;
//import org.springframework.stereotype.Component;
//import org.springframework.web.socket.WebSocketSession;
//
//import java.text.MessageFormat;
//import java.util.Map;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.concurrent.atomic.AtomicBoolean;
//
//@Slf4j
//@Component
//public class ChatpgtHandler implements IAllMessageEvent {
//
//    @Override
//    public int weight() {
//        return 70;
//    }
//
//    @Override
//    public String funName() {
//        return "ChatGPT";
//    }
//
////    private static final CacheMap<String,StateChatbot> CHATBOT_CACHE = new CacheMap<>(20, TimeUnit.HOURS,50);
//
//    private static final Map<String,StateChatbot> CHATBOT_CACHE = new ConcurrentHashMap<>();
//
//    private String key(final Message message){
//        return String.valueOf(message.getSelfId()) + getId(message);
//    }
//
//    private Long getId(final Message message){
//        Long id = 0L;
//        if(MessageTypeEnum.group.getType().equals(message.getMessageType())){
//            id = message.getGroupId();
//        }else if(MessageTypeEnum.privat.getType().equals(message.getMessageType())){
//            id = message.getUserId();
//        }
//        return id;
//    }
//
//    @Override
//    public boolean onMessage(final WebSocketSession session,final Message message,final String command) {
//        String text = CommonUtil.commandReplaceFirst(command,RegexEnum.CHATGPT);
//        if (Strings.isBlank(text)) {
//            return false;
//        }
//
//        if(!ChatgptConfig.support()){
//            Server.sendMessage(session,message.getUserId(),message.getGroupId(),message.getMessageType(),
//                    "请联系bot管理员qq配置ChatGPT session-token cf ua 或 邮箱和密码！",true);
//            return true;
//        }
//        // 先暂时只处理有session-token的情况
//
//        String key = key(message);
//        StateChatbot item = CHATBOT_CACHE.get(key);
//        if(item == null){
//            try {
//                StateChatbot chatbot = new StateChatbot(message,ChatgptConfig.SESSION_TOKEN,ChatgptConfig.CF_CLEARANCE,ChatgptConfig.USER_AGENT);
//                CHATBOT_CACHE.put(key,chatbot);
//                item = chatbot;
//            }catch (Exception e){
//                log.error("创建会话异常",e);
//                Server.sendMessage(session,message.getUserId(),message.getGroupId(),message.getMessageType(),
//                        "创建会话异常："+e.getMessage(),true);
//                return true;
//            }
//        }else{
//            if(item.getSending(message)){
//                Server.sendMessage(session,message.getUserId(),message.getGroupId(),message.getMessageType(),
//                        "您的ChatGPT正在处理中。。",true);
//
//              return true;
//            }
//        }
//        ThreadPoolUtil.getHandleCommandPool().execute(new Task(session,message,text,item));
//        return true;
//    }
//
//
//    private class Task implements Runnable{
//
//        private WebSocketSession session;
//        private Message message;
//        private StateChatbot chatbot;
//        private String text;
//
//        public Task(WebSocketSession session,Message message,String text,StateChatbot chatbot){
//            this.session = session;
//            this.message = message;
//            this.text = text;
//            chatbot.setSending(message,true);
//            this.chatbot = chatbot;
//        }
//
//        @Override
//        public void run() {
//            try {
//                Map<String, Object> chatResponse = chatbot.getChatResponse(text);
//                Object reply = chatResponse.get("message");
//                if(reply instanceof String){
//                    Server.sendMessage(session,message.getUserId(),message.getGroupId(),message.getMessageType(),String.valueOf(reply),true);
//                }else {
//                    log.error("reply非string类型,reply：{}",reply);
//                }
//            }catch (Exception e){
//                log.error("ChatGPT异常",e);
//                CHATBOT_CACHE.remove(key(message));
//                chatbot = null;
//                Server.sendMessage(session,message.getUserId(),message.getGroupId(),message.getMessageType(),
//                        MessageFormat.format("ChatGPT异常,清空会话\n{0}",e.getMessage()),true);
//            }finally {
//                if(chatbot != null){
//                    chatbot.setSending(message,false);
//                }
//            }
//        }
//    }
//
//
//}
//
//@Slf4j
//class StateChatbot extends Chatbot{
//
//    // 私聊使用
//    private AtomicBoolean privateSending;
//    // 群聊用
//    private Map<String,AtomicBoolean> groupSending;
//
//
//    public StateChatbot(final Message message,String sessionToken,String cfClearance,String userAgent) throws Exception{
//        super(sessionToken,cfClearance,userAgent);
//        initSending(message,false);
//        log.info("开启新的会话，userId：{}，groupId：{}",message.getUserId(),message.getGroupId());
//    }
//
//    public boolean getSending(final Message message){
//        if(MessageTypeEnum.group.getType().equals(message.getMessageType())){
//            AtomicBoolean atomicBoolean = groupSending.get(key(message));
//            if(atomicBoolean != null){
//                return atomicBoolean.get();
//            }
//        }else if(MessageTypeEnum.privat.getType().equals(message.getMessageType())){
//            return privateSending.get();
//        }
//        return false;
//    }
//
//    private void initSending(final Message message, boolean sending){
//        if(MessageTypeEnum.group.getType().equals(message.getMessageType())){
//            groupSending = new ConcurrentHashMap<>();
//            groupSending.put(key(message),new AtomicBoolean(sending));
//        }else if(MessageTypeEnum.privat.getType().equals(message.getMessageType())){
//            privateSending = new AtomicBoolean(sending);
//        }
//    }
//
//    public void setSending(final Message message,boolean sending){
//        if(MessageTypeEnum.group.getType().equals(message.getMessageType())){
//            AtomicBoolean atomicBoolean = groupSending.get(key(message));
//            if(atomicBoolean != null){
//                atomicBoolean.set(sending);
//            }
//        }else if(MessageTypeEnum.privat.getType().equals(message.getMessageType())){
//            this.privateSending.set(sending);
//        }
//    }
//
//    private String key(final Message message){
//        return message.getSelfId() + String.valueOf(message.getGroupId()) + message.getUserId();
//    }
//
//
//}
