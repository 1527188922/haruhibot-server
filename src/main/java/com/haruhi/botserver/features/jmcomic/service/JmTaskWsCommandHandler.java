package com.haruhi.botserver.features.jmcomic.service;

import com.haruhi.botserver.infrastructure.web.websocket.WebuiWsCommandContext;
import com.haruhi.botserver.infrastructure.web.websocket.WebuiWsCommandHandler;
import org.springframework.stereotype.Component;

/**
 * JM任务的WebSocket命令
 * <p>
 * 只提供"拉取当前快照"，取消任务仍然走原有的HTTP接口(已实现且已测)，
 * 取消后由 {@link JmTaskPushService} 推送最新快照
 */
@Component
public class JmTaskWsCommandHandler implements WebuiWsCommandHandler {

    private final JmcomicService jmcomicService;

    public JmTaskWsCommandHandler(JmcomicService jmcomicService) {
        this.jmcomicService = jmcomicService;
    }

    @Override
    public boolean supports(String type) {
        return JmTaskPushService.COMMAND_LIST.equals(type);
    }

    @Override
    public void handle(WebuiWsCommandContext context) {
        context.reply(JmTaskPushService.MESSAGE_TYPE_SNAPSHOT, jmcomicService.listTasks());
    }
}
