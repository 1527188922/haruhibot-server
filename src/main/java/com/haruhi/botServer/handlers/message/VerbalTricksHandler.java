package com.haruhi.botServer.handlers.message;

import com.haruhi.botServer.dto.gocq.response.Message;
import com.haruhi.botServer.entity.VerbalTricks;
import com.haruhi.botServer.event.message.IAllMessageEvent;
import com.haruhi.botServer.utils.ThreadPoolUtil;
import com.haruhi.botServer.utils.CommonUtil;
import com.haruhi.botServer.ws.Server;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 话术
 * 这个功能其实就是在所有群都生效的词条功能
 */
@Slf4j
@Component
public class VerbalTricksHandler implements IAllMessageEvent {
    @Override
    public int weight() {
        return 2;
    }

    @Override
    public String funName() {
        return "话术";
    }

    private final static Map<String, List<VerbalTricks>> cache = new HashMap<>();
    public static void putAllCache(Map<String, List<VerbalTricks>> other){
        cache.putAll(other);
    }

    public static void clearCache(){
        cache.clear();
    }

    @Override
    public boolean onMessage(WebSocketSession session,Message message, String command) {
        if(cache.size() == 0 || !message.isTextMsg()){
            return false;
        }
        if (message.isAtBot()) {
            command = message.getText(0).trim();
        }

        List<VerbalTricks> answerObj = null;
        for (Map.Entry<String, List<VerbalTricks>> item : cache.entrySet()) {
            if (command.matches(item.getKey())) {
                answerObj = item.getValue();
                break;
            }
        }
        if(answerObj == null){
            return false;
        }
        ThreadPoolUtil.getHandleCommandPool().execute(new Task(session,answerObj,message));
        return true;
    }

    @AllArgsConstructor
    private static class Task implements Runnable{
        private WebSocketSession session;
        private List<VerbalTricks>  answerObj;
        private Message message;


        @Override
        public void run() {
            if (answerObj.size() == 1) {
                Server.sendMessage(session,message.getUserId(),message.getGroupId(),message.getMessageType(),answerObj.get(0).getAnswer(),true);
            }else{
                VerbalTricks verbalTricks = answerObj.get(CommonUtil.randomInt(0, answerObj.size() - 1));
                Server.sendMessage(session,message.getUserId(),message.getGroupId(),message.getMessageType(),verbalTricks.getAnswer(),true);
            }
        }
    }
}
