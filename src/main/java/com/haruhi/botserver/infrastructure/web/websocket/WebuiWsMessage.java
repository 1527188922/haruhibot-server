package com.haruhi.botserver.infrastructure.web.websocket;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * webui WebSocket 消息信封，收发都用这个结构
 * <p>
 * 客户端发起请求时带上 id，服务端原样回传，前端据此把请求和响应配对
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebuiWsMessage {

    /**
     * 客户端 -> 服务端：心跳
     */
    public static final String TYPE_PING = "ping";
    /**
     * 服务端 -> 客户端：心跳响应
     */
    public static final String TYPE_PONG = "pong";
    /**
     * 客户端 -> 服务端：订阅/取消订阅主题
     */
    public static final String TYPE_SUBSCRIBE = "subscribe";
    public static final String TYPE_UNSUBSCRIBE = "unsubscribe";
    /**
     * 服务端 -> 客户端：订阅成功
     */
    public static final String TYPE_SUBSCRIBED = "subscribed";
    /**
     * 服务端 -> 客户端：握手成功
     */
    public static final String TYPE_CONNECTED = "connected";
    /**
     * 服务端 -> 客户端：命令执行失败
     */
    public static final String TYPE_ERROR = "error";

    private String type;
    private String id;
    private Long ts;
    private String message;
    private Object data;

    public static WebuiWsMessage of(String type, Object data) {
        return new WebuiWsMessage(type, null, System.currentTimeMillis(), null, data);
    }

    public static WebuiWsMessage reply(String type, String id, Object data) {
        return new WebuiWsMessage(type, id, System.currentTimeMillis(), null, data);
    }

    public static WebuiWsMessage error(String id, String message) {
        return new WebuiWsMessage(TYPE_ERROR, id, System.currentTimeMillis(), message, null);
    }

    public String toJson() {
        return JSON.toJSONString(this);
    }

    public static WebuiWsMessage parse(String json) {
        return JSON.parseObject(json, WebuiWsMessage.class);
    }

    /**
     * data 字段按对象读取，非对象时返回空对象
     */
    public JSONObject dataAsObject() {
        if (data instanceof JSONObject jsonObject) {
            return jsonObject;
        }
        return new JSONObject();
    }
}
