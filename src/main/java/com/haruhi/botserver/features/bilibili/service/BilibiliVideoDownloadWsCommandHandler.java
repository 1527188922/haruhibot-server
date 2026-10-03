package com.haruhi.botserver.features.bilibili.service;

import com.haruhi.botserver.infrastructure.web.websocket.WebuiWsCommandContext;
import com.haruhi.botserver.infrastructure.web.websocket.WebuiWsCommandHandler;
import org.springframework.stereotype.Component;

/**
 * b站视频下载任务的WebSocket命令
 * <p>
 * 只提供"拉取当前快照"，开始/取消下载仍然走HTTP接口，变化由
 * {@link BilibiliVideoDownloadPushService} 推送
 */
@Component
public class BilibiliVideoDownloadWsCommandHandler implements WebuiWsCommandHandler {

    private final BilibiliVideoDownloadService downloadService;

    public BilibiliVideoDownloadWsCommandHandler(BilibiliVideoDownloadService downloadService) {
        this.downloadService = downloadService;
    }

    @Override
    public boolean supports(String type) {
        return BilibiliVideoDownloadPushService.COMMAND_LIST.equals(type);
    }

    @Override
    public void handle(WebuiWsCommandContext context) {
        context.reply(BilibiliVideoDownloadPushService.MESSAGE_TYPE_SNAPSHOT, downloadService.snapshot());
    }
}
