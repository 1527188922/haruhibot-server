package com.haruhi.botserver.integration.onebot.websocket;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 机器人反向ws接入状态
 * <p>
 * 逻辑启停只影响是否接受新连接，进程与端口始终在运行（握手拦截器据此拒绝连接）
 */
@Component
public class BotWsState {

    private final AtomicBoolean running = new AtomicBoolean(true);

    public boolean isRunning() {
        return running.get();
    }

    public void start() {
        running.set(true);
    }

    public void stop() {
        running.set(false);
    }
}
