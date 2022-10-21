package com.haruhi.botServer.handlers.message;

import com.haruhi.botServer.constant.RegexEnum;
import com.haruhi.botServer.dto.gocq.response.Message;
import com.haruhi.botServer.entity.VerbalTricks;
import com.haruhi.botServer.event.message.IMessageEvent;
import com.haruhi.botServer.factory.ThreadPoolFactory;
import com.haruhi.botServer.utils.CommonUtil;
import com.haruhi.botServer.ws.Server;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 话术
 * 这个功能其实就是在所有群都生效的词条功能
 */
@Slf4j
@Component
public class VerbalTricksHandler implements IMessageEvent {
    @Override
    public int weight() {
        return 2;
    }

    @Override
    public String funName() {
        return "话术";
    }

    private static Map<String, List<VerbalTricks>> cache = new ConcurrentHashMap<>();
    public static void setCache(Map<String, List<VerbalTricks>> cache){
        VerbalTricksHandler.cache = cache;
    }

    @Override
    public boolean onMessage(final WebSocketSession session,final Message message, final String command) {
        if(cache.size() == 0){
            return false;
        }
        String cmd = new String(command);
        if (CommonUtil.isAt(message.getSelf_id(),command)) {
            cmd = cmd.replaceAll(RegexEnum.CQ_CODE_REPLACR.getValue(), "").replace(" ","");
        }

        List<VerbalTricks> answerObj = null;
        for (Map.Entry<String, List<VerbalTricks>> item : this.cache.entrySet()) {
            if (cmd.matches(item.getKey())) {
                answerObj = item.getValue();
                break;
            }
        }
        if(answerObj == null){
            return false;
        }
        ThreadPoolFactory.getCommandHandlerThreadPool().execute(new Task(session,message,answerObj));
        return true;
    }

    private class Task implements Runnable{
        private WebSocketSession session;
        private List<VerbalTricks>  answerObj;
        private Message message;

        public Task(WebSocketSession session,Message message,List<VerbalTricks> answerObj){
            this.session = session;
            this.answerObj = answerObj;
            this.message = message;
        }


        @Override
        public void run() {
            if (answerObj.size() == 1) {
                Server.sendMessage(session,message.getUser_id(),message.getGroup_id(),message.getMessage_type(),answerObj.get(0).getAnswer(),true);
            }else{
                VerbalTricks verbalTricks = answerObj.get(CommonUtil.randomInt(0, answerObj.size() - 1));
                Server.sendMessage(session,message.getUser_id(),message.getGroup_id(),message.getMessage_type(),verbalTricks.getAnswer(),true);
            }
        }
    }
}
