package com.haruhi.botserver.infrastructure.web.websocket;

/**
 * webui WebSocket 自定义关闭码
 * <p>
 * 前端据此区分"需要重新登录"和"正常断开"，避免token失效后无限重连
 */
public final class WebuiWsCloseCodes {

    /**
     * 登录态失效
     */
    public static final int AUTH_EXPIRED = 4001;
    /**
     * 主动登出或被服务端踢下线
     */
    public static final int LOGOUT = 4002;
    /**
     * 服务端内部错误(发送失败等)
     */
    public static final int SERVER_ERROR = 4003;
    /**
     * 服务停止
     */
    public static final int GOING_AWAY = 4004;

    private WebuiWsCloseCodes() {
    }
}
