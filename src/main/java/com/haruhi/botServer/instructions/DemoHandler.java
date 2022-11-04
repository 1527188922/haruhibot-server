package com.haruhi.botServer.instructions;

import com.haruhi.botServer.cache.CacheMap;
import com.haruhi.botServer.dto.gocq.response.Message;
import com.haruhi.botServer.event.message.IMessageEvent;
import com.haruhi.botServer.ws.Server;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class DemoHandler implements IMessageEvent {

    private final static CacheMap<String,Node> nodeCache = new CacheMap<>(60L, TimeUnit.SECONDS,500);
    private static List<Node> nodes;

    public DemoHandler(){
        nodes = createNodes();
    }


    private List<Node> createNodes(){
        List<Node> nodes = new ArrayList<>();

        /***
         * 自定义每个节点
         * 可自定义的项: 1.匹配规则(IMatcher) 2:要做的事情(Runnable) 3:节点携带的自定义数据(泛型 T)
         */
        String customData = "滴滴滴";
        new Node<String>(customData, (session, message) -> {
            // 这里是自定义匹配规则 true:标识匹配上命令,将执行runnable.run()内容
            return false;
        }, new Task() {
            @Override
            public void run(WebSocketSession session, Message message) {

            }

            @Override
            public void run() {

            }
        });
        // 这里是当前节点要做的事情
        log.info("当前节点自定义的数据是:{}",customData);

//        Server.sendMessage();
        return nodes;
    }

    private <T> void putCache(final Message message,Node<T> node){
        DemoHandler.nodeCache.put(key(message),node);
    }
    private <T> Node getNode(final Message message){
        return nodeCache.get(key(message));
    }

    private String key(final Message message){
        return String.valueOf(message.getSelf_id()) + message.getGroup_id() + String.valueOf(message.getUser_id());
    }



    @Override
    public int weight() {
        return 79;
    }

    @Override
    public String funName() {
        return "指令匹配demo";
    }

    @Override
    public boolean onMessage(final WebSocketSession session,final Message message,final String command) {




        return false;
    }


}
