package com.haruhi.botserver.bot.handler;

import com.haruhi.botserver.bot.session.Bot;
import com.haruhi.botserver.integration.onebot.model.NoticeTypeEnum;
import com.haruhi.botserver.integration.onebot.model.event.NoticeEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 机器人下线通知处理
 * 下线状态已由 NoticeDispatcher 标记，这里预留扩展点（如通知管理员、自动重连等）
 */
@Slf4j
@Component
public class BotOfflineHandler implements INoticeHandler {

    @Override
    public boolean support(NoticeEvent event) {
        return NoticeTypeEnum.bot_offline.name().equals(event.getNoticeType());
    }

    @Override
    public void onNotice(Bot bot, NoticeEvent event) {

    }
}
