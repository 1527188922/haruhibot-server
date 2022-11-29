package com.haruhi.botServer.handlers.message.wordStrip;

import com.haruhi.botServer.dto.gocq.response.Message;
import com.haruhi.botServer.event.message.IGroupMessageEvent;
import com.haruhi.botServer.utils.ThreadPoolUtil;
import com.haruhi.botServer.ws.Server;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class WordStripHandler implements IGroupMessageEvent {

    private static Map<String,String> cache = new ConcurrentHashMap<>();
    public static void putCache(Long selfId,Long groupId,String keyWord,String answer){
        cache.put(getKey(selfId,groupId,keyWord),answer);
    }
    public static void removeCache(Long selfId,Long groupId,String keyWord){
        cache.remove(getKey(selfId,groupId,keyWord));
    }
    public static String getKey(Long selfId,Long groupId,String keyWord){
        return selfId + "-" + groupId + "-" + keyWord;
    }


    @Override
    public int weight() {
        return 95;
    }

    @Override
    public String funName() {
        return "词条监听";
    }

    @Override
    public boolean onGroup(final WebSocketSession session,final Message message, final String command) {
        String answer = cache.get(getKey(message.getSelfId(),message.getGroupId(),command));
        if(answer == null){
            return false;
        }
        ThreadPoolUtil.getHandleCommandPool().execute(()->{
            Server.sendGroupMessage(session,message.getGroupId(),answer,false);
        });
        return true;
    }
}
