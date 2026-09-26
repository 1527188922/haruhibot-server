package com.haruhi.botserver.bootstrap;

import lombok.extern.slf4j.Slf4j;

/**
 * 程序级固定常量
 * <p>
 * 这些值在编译期就确定，不参与配置管理，也不允许运行期修改。
 * 运行期可变的配置请一律使用 {@link com.haruhi.botserver.configuration.service.Configs}。
 */
@Slf4j
public class SysConstants {

    public static final String DEFAULT_NAME = "春日酱";

    public static final String CONTEXT_PATH = "/api";
    public static final String DRUID_PATH = "/druid";
    public static final String WEB_SOCKET_PATH = CONTEXT_PATH + "/ws";
    /**
     * webui 全局WebSocket(与机器人反向ws相互独立)
     * <p>
     * 必须位于 {@link #CONTEXT_PATH} 之下：VueHistoryFilter 会把非 /api 开头的GET请求转发到 index.html
     */
    public static final String WEBUI_WEB_SOCKET_PATH = CONTEXT_PATH + "/webui/ws";
}
