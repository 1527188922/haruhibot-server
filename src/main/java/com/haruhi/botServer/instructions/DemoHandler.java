package com.haruhi.botServer.instructions;

import com.haruhi.botServer.cache.CacheMap;
import com.haruhi.botServer.dto.gocq.response.Message;
import com.haruhi.botServer.event.message.IMessageEvent;
import com.haruhi.botServer.ws.Server;
import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.socket.WebSocketSession;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;


/**
 * 1如何自定义创建节点  2如何遍历匹配节点 3如何保存/记录执行节点
 * 以上三点 根据不同的Handler或不同的功能需求自定义
 *
 * 比如这个DemoHandler，我使用nodeCache保存/记录执行节点，这是个有过期时间的cache
 * 过期则回到根节点开始
 *
 * 如果当前进度中的节点一个都没有匹配上，那么onMessage方法返回false，继续向下匹配其他handler
 *
 */
//@Component // 若放开注释 则启用该功能
@Slf4j
public class DemoHandler implements IMessageEvent {

    // 一分钟过期
    private final static CacheMap<String, RunnableNode> nodeCache = new CacheMap<String, RunnableNode>(60L, TimeUnit.SECONDS,500);

    private <T> void putCache(final Message message,final RunnableNode<T> node){
        DemoHandler.nodeCache.put(key(message),node);
    }
    private void removeCache(final Message message){
        DemoHandler.nodeCache.remove(key(message));
    }
    private <T> RunnableNode<T> getNode(final Message message){
        return nodeCache.get(key(message));
    }

    private String key(final Message message){
        return key(message.getSelf_id(),message.getGroup_id(),message.getUser_id());
    }

    /**
     * 唯一key
     * @param selfId 机器人qq
     * @param groupId 群号
     * @param userId 发送人
     * @return
     */
    private String key(Long selfId,Long groupId,Long userId){
        return selfId + "-" + groupId + "-" + userId;
    }


    /**
     * 只用来存放根节点
     */
    private static List<RunnableNode> roots;

    public DemoHandler(){
        roots = createNodes();
    }


    /**
     * 这个方法用来自定义指令树 一个root表示一棵树,根据业务需求 可以存在多个根节点 也就是多棵树
     * 树的结构和每个节点的匹配规则以及每个节点要做的事情都可以自定义
     * 这是我创建的demo 结果看流程图 https://www.processon.com/view/link/6365e141e401fd612f4b167c
     * @return
     */
    private List<RunnableNode> createNodes(){
        List<RunnableNode> bootstrap = new ArrayList<>();

        /***
         * 自定义每个节点
         * 可自定义的项: 1.匹配规则(matches) 2:要做的事情(run) 3:节点携带的自定义数据(泛型 T)
         */

        // -------------定义3个根节点
        // 第1个根节点
        String nodeA1CustomData = "a1";
        RunnableNode<String> nodeA1 = new RunnableNode<String>(nodeA1CustomData){
//            @Override
//            protected boolean matches(final WebSocketSession session,final Message message)throws Exception {
//                // 在这个方法里，你可以随便定义当前节点的匹配规则 最终返回true就表示匹配成功，将执行run方法
//                String data = getData();
//                log.info("这是当前节点自定的数据：{}",data);
//                return data.equals(message.getMessage());
//            }

            @Override
            protected boolean run(final WebSocketSession session,final Message message) throws Exception{
                String d = getData();
                if (!d.equals(message.getMessage())) {
                    return false;
                }
                log.info("这是当前节点自定的数据：{}",d);
                Server.sendMessage(session,message.getUser_id(),message.getGroup_id(),message.getMessage_type(),
                        "我是a1",true);
                return true;
            }
        };
        bootstrap.add(nodeA1); // 将定义的根节点添加到根节点集合中

        // 第2个根节点
        String nodeA2CustomData = "a2";
        RunnableNode<String> nodeA2 = new RunnableNode<String>(nodeA2CustomData){
//            @Override
//            protected boolean matches(final WebSocketSession session,final Message message) throws Exception{
//                // 在这个方法里，你可以随便定义当前节点的匹配规则 最终返回true就表示匹配成功，将执行run方法
//                String data = getData();
//                log.info("这是当前节点自定的数据：{}",data);
//                return data.equals(message.getMessage());
//            }

            @Override
            protected boolean run(final WebSocketSession session,final Message message)throws Exception {
                String d = getData();
                if (!d.equals(message.getMessage())) {
                    return false;
                }
                log.info("这是当前节点自定的数据：{}",d);
                Server.sendMessage(session,message.getUser_id(),message.getGroup_id(),message.getMessage_type(),
                        "我是a2",true);
                return true;
            }
        };
        bootstrap.add(nodeA2);

        // 第3个根节点
        String nodeA3CustomData = "a3";
        RunnableNode<String> nodeA3 = new RunnableNode<String>(nodeA3CustomData){
//            @Override
//            protected boolean matches(final WebSocketSession session,final Message message)throws Exception {
//                // 在这个方法里，你可以随便定义当前节点的匹配规则 最终返回true就表示匹配成功，将执行run方法
//                String data = getData();
//                log.info("这是当前节点自定的数据：{}",data);
//                return data.equals(message.getMessage());
//            }

            @Override
            protected boolean run(final WebSocketSession session,final Message message) throws Exception{
                String d = getData();
                if (!d.equals(message.getMessage())) {
                    return false;
                }
                log.info("这是当前节点自定的数据：{}",d);
                Server.sendMessage(session,message.getUser_id(),message.getGroup_id(),message.getMessage_type(),
                        "我是a3",true);
                return true;
            }
        };
        bootstrap.add(nodeA3);

        // ------ 接下来自定义第2层级的节点

        String nodeB1CustomData = "b1";
        // b1是a1的一个子节点
        RunnableNode<String> nodeB1 = new RunnableNode<String>(nodeA1, nodeB1CustomData) {
//            @Override
//            protected boolean matches(final WebSocketSession session,final Message message) throws Exception{
//                // 在这个方法里，你可以随便定义当前节点的匹配规则 最终返回true就表示匹配成功，将执行run方法
//                String data = getData();
//                log.info("这是当前节点自定的数据：{}", data);
//                return data.equals(message.getMessage());
//            }

            @Override
            protected boolean run(final WebSocketSession session,final Message message) throws Exception{
                String d = getData();
                if (!d.equals(message.getMessage())) {
                    return false;
                }
                log.info("这是当前节点自定的数据：{}", d);
                Server.sendMessage(session, message.getUser_id(), message.getGroup_id(), message.getMessage_type(),
                        "我是b1", true);
                return true;
            }
        };


        // b2是a1的一个子节点
        String nodeB2CustomData = "b2";
        RunnableNode<String> nodeB2 = new RunnableNode<String>(nodeA1, nodeB2CustomData) {
//            @Override
//            protected boolean matches(final WebSocketSession session,final Message message)throws Exception {
//                // 在这个方法里，你可以随便定义当前节点的匹配规则 最终返回true就表示匹配成功，将执行run方法
//                String data = getData();
//                log.info("这是当前节点自定的数据：{}", data);
//                return data.equals(message.getMessage());
//            }

            @Override
            protected boolean run(final WebSocketSession session,final Message message)throws Exception {
                String d = getData();
                if (!d.equals(message.getMessage())) {
                    return false;
                }
                log.info("这是当前节点自定的数据：{}", d);
                Server.sendMessage(session, message.getUser_id(), message.getGroup_id(), message.getMessage_type(),
                        "我是b2", true);
                return true;
            }
        };

        // b3是a1的一个子节点
        String nodeB3CustomData = "b3";
        new RunnableNode<String>(nodeA1,nodeB3CustomData){ //终结点new出来可以不定义变量
//            @Override
//            protected boolean matches(final WebSocketSession session,final Message message)throws Exception {
//                // 在这个方法里，你可以随便定义当前节点的匹配规则 最终返回true就表示匹配成功，将执行run方法
//                String data = getData();
//                log.info("这是当前节点自定的数据：{}",data);
//                return data.equals(message.getMessage());
//            }

            @Override
            protected boolean run(final WebSocketSession session,final Message message) throws Exception{
                String d = getData();
                if (!d.equals(message.getMessage())) {
                    return false;
                }
                log.info("这是当前节点自定的数据：{}",d);
                Server.sendMessage(session,message.getUser_id(),message.getGroup_id(),message.getMessage_type(),
                        "我是b3",true);
                return true;
            }
        };


        // b4是a2的一个子节点
        String nodeB4CustomData = "b4";
        new RunnableNode<String>(nodeA2,nodeB4CustomData){
//            @Override
//            protected boolean matches(final WebSocketSession session,final Message message) throws Exception{
//                // 在这个方法里，你可以随便定义当前节点的匹配规则 最终返回true就表示匹配成功，将执行run方法
//                String data = getData();
//                log.info("这是当前节点自定的数据：{}",data);
//                return data.equals(message.getMessage());
//            }

            @Override
            protected boolean run(final WebSocketSession session,final Message message) throws Exception{
                String d = getData();
                if (!d.equals(message.getMessage())) {
                    return false;
                }
                log.info("这是当前节点自定的数据：{}",d);
                Server.sendMessage(session,message.getUser_id(),message.getGroup_id(),message.getMessage_type(),
                        "我是b4",true);
                return true;
            }
        };

        // b5是a2的一个子节点
        String nodeB5CustomData = "b5";
        new RunnableNode<String>(nodeA2,nodeB5CustomData){
//            @Override
//            protected boolean matches(final WebSocketSession session,final Message message)throws Exception {
//                // 在这个方法里，你可以随便定义当前节点的匹配规则 最终返回true就表示匹配成功，将执行run方法
//                String data = getData();
//                log.info("这是当前节点自定的数据：{}",data);
//                return data.equals(message.getMessage());
//            }

            @Override
            protected boolean run(final WebSocketSession session,final Message message) throws Exception {
                String d = getData();
                if (!d.equals(message.getMessage())) {
                    return false;
                }
                log.info("这是当前节点自定的数据：{}",d);
                Server.sendMessage(session,message.getUser_id(),message.getGroup_id(),message.getMessage_type(),
                        "我是b5",true);
                return true;
            }
        };

        // c1是b1的一个子节点
        String nodeC1CustomData = "c1";
        RunnableNode<String> nodeC1 = new RunnableNode<String>(nodeB1, nodeC1CustomData) {
//            @Override
//            protected boolean matches(final WebSocketSession session,final Message message) throws Exception{
//                // 在这个方法里，你可以随便定义当前节点的匹配规则 最终返回true就表示匹配成功，将执行run方法
//                String data = getData();
//                log.info("这是当前节点自定的数据：{}", data);
//                return data.equals(message.getMessage());
//            }

            @Override
            protected boolean run(final WebSocketSession session,final Message message)throws Exception {
                String d = getData();
                if (!d.equals(message.getMessage())) {
                    return false;
                }
                log.info("这是当前节点自定的数据：{}", d);
                Server.sendMessage(session, message.getUser_id(), message.getGroup_id(), message.getMessage_type(),
                        "我是c1", true);
                return true;
            }
        };

        // c2是b1的一个子节点
        String nodeC2CustomData = "c2";
        new RunnableNode<String>(nodeB1,nodeC2CustomData){
//            @Override
//            protected boolean matches(final WebSocketSession session,final Message message)throws Exception {
//                // 在这个方法里，你可以随便定义当前节点的匹配规则 最终返回true就表示匹配成功，将执行run方法
//                String data = getData();
//                log.info("这是当前节点自定的数据：{}",data);
//                return data.equals(message.getMessage());
//            }

            @Override
            protected boolean run(final WebSocketSession session,final Message message) throws Exception{
                String d = getData();
                if (!d.equals(message.getMessage())) {
                    return false;
                }
                log.info("这是当前节点自定的数据：{}",d);
                Server.sendMessage(session,message.getUser_id(),message.getGroup_id(),message.getMessage_type(),
                        "我是c2",true);
                return true;
            }
        };

        // c3是b2的一个子节点
        String nodeC3CustomData = "c3";
        new RunnableNode<String>(nodeB2,nodeC3CustomData){
//            @Override
//            protected boolean matches(final WebSocketSession session,final Message message)throws Exception {
//                // 在这个方法里，你可以随便定义当前节点的匹配规则 最终返回true就表示匹配成功，将执行run方法
//                String data = getData();
//                log.info("这是当前节点自定的数据：{}",data);
//                return data.equals(message.getMessage());
//            }

            @Override
            protected boolean run(final WebSocketSession session,final Message message) throws Exception{
                String d = getData();
                if (!d.equals(message.getMessage())) {
                    return false;
                }
                log.info("这是当前节点自定的数据：{}",d);
                Server.sendMessage(session,message.getUser_id(),message.getGroup_id(),message.getMessage_type(),
                        "我是c3",true);
                return true;
            }
        };

        // d1是c1的一个子节点
        String nodeD1CustomData = "d1";
        new RunnableNode<String>(nodeC1,nodeD1CustomData){
//            @Override
//            protected boolean matches(final WebSocketSession session,final Message message) throws Exception{
//                // 在这个方法里，你可以随便定义当前节点的匹配规则 最终返回true就表示匹配成功，将执行run方法
//                String data = getData();
//                log.info("这是当前节点自定的数据：{}",data);
//                return data.equals(message.getMessage());
//            }

            @Override
            protected boolean run(final WebSocketSession session,final Message message) throws Exception {
                String d = getData();
                if (!d.equals(message.getMessage())) {
                    return false;
                }
                log.info("这是当前节点自定的数据：{}",d);
                Server.sendMessage(session,message.getUser_id(),message.getGroup_id(),message.getMessage_type(),
                        "我是d1",true);
                return true;
            }
        };

        // 只需要返回根节点集合
        return bootstrap;
    }


    @Override
    public int weight() {
        // 节点数如果较多 那么该handler的优先级建议放低一些
        return 49;
    }

    @Override
    public String funName() {
        return "指令匹配demo";
    }

    @Override
    public boolean onMessage(final WebSocketSession session,final Message message,final String command) {
        if(CollectionUtils.isEmpty(roots)){
            return false;
        }

        return ergodicNodes(session,message);
    }

    // ------------ ---------------    ------------------- --------------------

    /**
     * 判断从缓存节点的子节点集合从执行
     * 还是从根节点集合执行
     * @param session
     * @param message
     * @return
     */
    private boolean ergodicNodes(final WebSocketSession session,final Message message){
        // 先查找缓存是否存在该用户的执行记录 实际就是查一个node对象
        RunnableNode currentNode = getNode(message);
        List<RunnableNode> nodes = null;
        if (currentNode != null) {
            // 存在执行记录 从记录的子节点集合匹配
            nodes = currentNode.getChildNodes();
        }else{
            // 缓存中不存在该用户的执行节点记录 从根节点集合匹配命令
            nodes = roots;
        }

        boolean execute = execute(session, message, nodes);
        if (execute) {
            return true;
        }

        return false;
    }

    /**
     * 循环匹配执行同一层级的节点(循环调用run())
     * @param session
     * @param message
     * @param nodes 同一层级的节点集合
     * @return
     */
    private boolean execute(final WebSocketSession session,final Message message,final List<RunnableNode> nodes){
        for (RunnableNode node: nodes) {
            boolean execute = false;
            try {
                execute = node.run(session, message);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (execute) {
                // execute==true 表示该节点的matches方法返回true 说明匹配成功 将任务提交到了线程池执行
                missionAccomplished(session,message,node);
                return true;
            }
        }
        return false;
    }

    /**
     * 执行完成之后做的事
     * @param session
     * @param message
     * @param currentNode 被执行的节点
     */
    private void missionAccomplished(final WebSocketSession session,final Message message,final RunnableNode currentNode){
        List childNodes = currentNode.getChildNodes();
        if(!CollectionUtils.isEmpty(childNodes)){
            // 当前执行的节点还存在子节点 将当前执行的节点存到缓存
            putCache(message,currentNode);
        }else{
//
//            String tip = "你已执行完终节点指令，接下来将重新回到根节点";
//            if (currentNode.getParentNode() == null) {
//                // 节点没有父节点 表示当前节点为根节点
//                tip = "你所完成的根节点指令之下没有其他指令了，接下来将重新回根节点";
//            }
            Server.sendMessage(session,message.getUser_id(),message.getGroup_id(),message.getMessage_type(),
                    "终节点执行完成",true);
            log.info("终节点执行完成");
            removeCache(message);
        }
    }


}
