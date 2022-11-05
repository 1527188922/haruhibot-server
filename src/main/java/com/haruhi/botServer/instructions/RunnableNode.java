package com.haruhi.botServer.instructions;

import com.haruhi.botServer.dto.gocq.response.Message;
import com.haruhi.botServer.utils.ThreadPoolUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import org.springframework.web.socket.WebSocketSession;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public abstract class RunnableNode<T>{

    // 当前节点自定义的数据
    private T data;
    // 当前节点的父节点 为null表示当前节点是根节点
    private RunnableNode<T> parentNode;
    // 当前节点的子节点 为空表示当前节点是终节点
    private volatile List<RunnableNode<T>> childNodes = new LinkedList<>();
    // 当前节点的高度/深度 为0表示为根节点
    private AtomicInteger height;


    /**
     * 创建根节点
     * @param data
     */
    public RunnableNode(final T data){

        this.data = data;
        this.height = new AtomicInteger(0);
    }


    /**
     * 创建子节点
     * @param parentNode
     * @param data
     */
    public RunnableNode(final RunnableNode<T> parentNode, final T data){
        setParentNode(parentNode);
        this.data = data;
    }

    public void setParentNode(final RunnableNode<T> parentNode){
        if (parentNode == null) {
            throw new NullPointerException("parentNode is null");
        }
        if(parentNode != this.parentNode){
            if(!checkParentNode(parentNode,this.childNodes)){
                throw new IllegalArgumentException("The parent node exists in the child node of the current node");
            }
            if (this.parentNode != null) {
                // 当前节点的父节点不为空 则先删除旧父节点的引用
                List<RunnableNode<T>> childNodes = this.parentNode.getChildNodes();
                if (!CollectionUtils.isEmpty(childNodes)) {
                    childNodes.remove(this);
                }
            }

            List<RunnableNode<T>> childNodes = parentNode.getChildNodes();
            childNodes.add(this);

            this.parentNode = parentNode;
            this.height = new AtomicInteger(parentNode.getHeight().get() + 1);
        }

    }

    /**
     * 检查将要设置的父节点是否在当前节点的子节点中
     * 不存在则表示验证通过
     * @param parentNode
     * @param nodes
     * @return
     */
    private boolean checkParentNode(final RunnableNode<T> parentNode, final List<RunnableNode<T>> nodes){
        if(CollectionUtils.isEmpty(nodes)){
            return true;
        }

        for (RunnableNode<T> node : nodes) {
            if (node == parentNode) {
                return false;
            }else {
                return checkParentNode(parentNode,node.getChildNodes());
            }
        }
        return true;
    }


    public List<RunnableNode<T>> getChildNodes(){
        return this.childNodes;
    }
    public void setChildNodes(List<RunnableNode<T>> childNodes){
        this.childNodes = childNodes;
    }

    public AtomicInteger getHeight(){
        return height;
    }

    public RunnableNode<T> getParentNode(){
        return parentNode;
    }

    public void setData(T data){
        this.data = data;
    }
    public T getData(){
        return data;
    }


    protected abstract boolean matches(final WebSocketSession session,final Message message) throws Exception;

    protected abstract void run(final WebSocketSession session,final Message message) throws Exception;

    public boolean execute(final WebSocketSession session,final Message message){
        boolean matches = false;
        try {
            // 调用自定义的匹配方法
            matches = matches(session, message);
        } catch (Exception e) {
            log.error("节点匹配异常",e);
        }
        if (matches) {
            // 若匹配成功 将run()提交到线程池执行
            ThreadPoolUtil.getHandleCommandPool().execute(()->{
                try {
                    run(session,message);
                }catch (Exception e){
                    log.error("节点任务执行异常");
                }
            });
        }
        return matches;
    }


}
