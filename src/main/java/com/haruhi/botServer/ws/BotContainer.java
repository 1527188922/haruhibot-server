package com.haruhi.botServer.ws;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class BotContainer {

    // <sessionId,Bot>
    private static final Map<String, Bot> BOT_MAP = new ConcurrentHashMap<>();

    public static int getConnections(){
        return BOT_MAP.size();
    }
    public static Bot getBotFirst(){
        if (getConnections() > 0) {
            return ((Bot) BOT_MAP.values().toArray()[0]);
        }
        return null;
    }

    public static void add(Long botId, WebSocketSession session){
        BOT_MAP.put(session.getId(), new Bot(botId, session));
    }

    public static Bot getBotBySession(WebSocketSession session){
        return BOT_MAP.get(session.getId());
    }

    public static Bot getBotById(Long botId){
        for (Map.Entry<String, Bot> entry : BOT_MAP.entrySet()) {
            if (entry.getValue() != null
                    && entry.getValue().getId() != null
                    && entry.getValue().getId().equals(botId)) {
                return entry.getValue();
            }
        }
        return null;
    }

    public static void removeClient(WebSocketSession session){
        removeClient(session.getId());
    }
    public static void removeClient(String sessionId){
        Bot bot = BOT_MAP.remove(sessionId);
        if(bot != null){
            log.info("客户端断开 botId:{}  当前连接数：{}",bot.getId(),getConnections());
            try {
                bot.close();
            }catch (Exception e){

            }
        }
    }

    public static void removeAllClient(){
        BOT_MAP.keySet().forEach(BotContainer::removeClient);
        BOT_MAP.clear();
    }
}
