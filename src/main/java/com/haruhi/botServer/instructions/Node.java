package com.haruhi.botServer.instructions;

import org.springframework.util.CollectionUtils;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class Node<T> {

    private T data;
    private IMatcher matcher;
    private Task task;
    private Node<T> parentNode;
    private  volatile List<Node<T>> childNodes = new LinkedList<>();
    private AtomicInteger height;

    public Node(final T data,final IMatcher matcher,final Task task){
        if (matcher == null || task == null) {
            throw new IllegalArgumentException("matcher or task is null");
        }
        this.data = data;
        this.matcher = matcher;
        this.task = task;
        this.height = new AtomicInteger(0);
    }

    public Node(final Node<T> parentNode, final T data, final IMatcher matcher,final Task task){
        if (parentNode == null || matcher == null || task == null) {
            throw new IllegalArgumentException("parentNode or matcher or task is null");
        }
        this.data = data;
        this.matcher = matcher;
        this.task = task;
        setParentNode(parentNode);
    }

    public void setParentNode(final Node<T> parentNode){
        if (this.parentNode != null) {
            // 当前节点的父节点不为空 则先删除旧父节点的引用
            List<Node<T>> childNodes = this.parentNode.getChildNodes();
            if (!CollectionUtils.isEmpty(childNodes)) {
                childNodes.remove(this);
            }
        }

        List<Node<T>> childNodes = parentNode.getChildNodes();
        childNodes.add(this);

        this.parentNode = parentNode;
        this.height = new AtomicInteger(parentNode.getHeight().get() - 1);
    }


    public List<Node<T>> getChildNodes(){
        return this.childNodes;
    }
    public void setChildNodes(List<Node<T>> childNodes){
        this.childNodes = childNodes;
    }

    public AtomicInteger getHeight(){
        return height;
    }

    public Node<T> getParentNode(){
        return parentNode;
    }

    public T getData(){
        return data;
    }
    public Runnable getTask(){
        return task;
    }
    public IMatcher getMatcher(){
        return matcher;
    }

}
