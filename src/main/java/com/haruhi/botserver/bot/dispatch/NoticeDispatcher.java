package com.haruhi.botserver.bot.dispatch;

import com.haruhi.botserver.bot.handler.INoticeHandler;
import com.haruhi.botserver.bot.session.Bot;
import com.haruhi.botserver.configuration.metadata.ConfigKey;
import com.haruhi.botserver.configuration.service.Configs;
import com.haruhi.botserver.integration.onebot.model.NoticeTypeEnum;
import com.haruhi.botserver.integration.onebot.model.event.NoticeEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * qq通知分发器
 * 通知将通过这个类分发给所有实现了接口 INoticeHandler 的类
 */
@Slf4j
@Component
public class NoticeDispatcher {

    private final List<INoticeHandler> container = new ArrayList<>();

    public NoticeDispatcher(Map<String, INoticeHandler> noticeHandlerMap) {
        if (!CollectionUtils.isEmpty(noticeHandlerMap)) {
            container.addAll(noticeHandlerMap.values());
            log.info("加载了{}个通知处理类", container.size());
        }
    }

    public void onEvent(final Bot bot, final NoticeEvent event) {
        if (event == null) {
            return;
        }
        if (NoticeTypeEnum.bot_offline.name().equals(event.getNoticeType())) {
            // 账号登录失效等下线通知，先标记状态，再交给处理器处理
            bot.markOffline();
            log.warn("机器人下线通知 tag：{} message：{}", event.getTag(), event.getMessage());
        } else {
            log.info("收到通知类消息：noticeType：{}，subType：{}，groupId：{}，userId：{}",
                    event.getNoticeType(), event.getSubType(), event.getGroupId(), event.getUserId());
        }
        boolean disableGroup = Configs.getBool(ConfigKey.BOT_SWITCH_DISABLE_GROUP);
        if (disableGroup && event.isGroupNotice()) {
            return;
        }
        for (INoticeHandler handler : container) {
            try {
                if (handler.support(event)) {
                    handler.onNotice(bot, event);
                }
            } catch (Exception e) {
                log.error("处理通知事件异常 noticeType:{} subType:{} handler:{}",
                        event.getNoticeType(), event.getSubType(), handler.getClass().getSimpleName(), e);
            }
        }
    }
}
