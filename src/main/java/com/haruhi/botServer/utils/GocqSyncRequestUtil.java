package com.haruhi.botServer.utils;

import com.alibaba.fastjson.JSONObject;
import com.haruhi.botServer.constant.GocqActionEnum;
import com.haruhi.botServer.dto.gocq.request.RequestBox;
import com.haruhi.botServer.dto.gocq.response.Message;
import com.haruhi.botServer.dto.gocq.response.SelfInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 用于请求gocq的数据
 * 并不用来做发送消息
 */
@Slf4j
public class GocqSyncRequestUtil {
    private GocqSyncRequestUtil(){}

    private static Map<String,JSONObject> resultMap = new ConcurrentHashMap<>();
    public static void putEchoResult(String key, JSONObject val){
        resultMap.put(key,val);
    }

    /**
     * 获取消息详情对象
     * @param session
     * @param messageId
     * @param timeout
     * @return
     */
    public static Message getMsg(WebSocketSession session,String messageId,long timeout){
        Map<String, Object> map = new HashMap<>(1);
        map.put("message_id",messageId);
        JSONObject jsonObject = sendSyncRequest(session, GocqActionEnum.GET_MSG, map, timeout);
        if (jsonObject != null) {
            return JSONObject.parseObject(jsonObject.getString("data"), Message.class);
        }
        return null;
    }


    public static SelfInfo getLoginInfo(WebSocketSession session,long timeout){
        JSONObject responseStr = sendSyncRequest(session, GocqActionEnum.GET_LOGIN_INGO,null,timeout);
        if (responseStr != null) {
            SelfInfo data = JSONObject.parseObject(responseStr.getString("data"), SelfInfo.class);
            return data;
        }
        return null;
    }


    /***
     * 发送同步消息
     * @param session 客户端session
     * @param action 终结点
     * @param params 参数
     * @param timeout 超时
     * @param <T>
     * @return
     */
    public static <T> JSONObject sendSyncRequest(WebSocketSession session, GocqActionEnum action, T params, long timeout){
        RequestBox<T> requestBox = new RequestBox<>();
        if(params != null){
            requestBox.setParams(params);
        }
        requestBox.setAction(action.getAction());
        String echo = Thread.currentThread().getName() + "_" + session.getId() + "_" + action.getAction() + "_" + CommonUtil.uuid();
        requestBox.setEcho(echo);
        try {
            session.sendMessage(new TextMessage(JSONObject.toJSONString(requestBox)));
            FutureTask<JSONObject> futureTask = new FutureTask<>(new GocqSyncRequestUtil.Task(echo));
            new Thread(futureTask).start();
            JSONObject res = futureTask.get(timeout, TimeUnit.MILLISECONDS);
            return res;
        }catch (InterruptedException e){
            log.error("发送同步消息线程中断异常,echo:{}",echo,e);
        } catch (ExecutionException e) {
            log.error("发送同步消息执行异常,echo:{}",echo,e);
        } catch (TimeoutException e) {
            log.error("发送同步消息超时,echo:{}",echo,e);
        } catch (IOException e) {
            log.error("发送同步消息IO异常,echo:{}",echo,e);
        }finally {
            resultMap.remove(echo);
        }
        return null;
    }

    private static class Task implements Callable<JSONObject> {
        private String echo;
        Task(String echo){
            this.echo = echo;
        }
        @Override
        public JSONObject call() throws Exception {
            JSONObject res = null;
            while (res == null){
                res = resultMap.get(echo);
            }
            return res;
        }
    }
}
