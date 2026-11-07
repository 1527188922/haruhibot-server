package com.haruhi.botserver.bot.dispatch;

import com.haruhi.botserver.bot.session.Bot;
import com.haruhi.botserver.features.contacts.service.FriendSqliteService;
import com.haruhi.botserver.features.contacts.service.GroupInfoSqliteService;
import com.haruhi.botserver.integration.onebot.model.MetaEventEnum;
import com.haruhi.botserver.integration.onebot.model.SubTypeEnum;
import com.haruhi.botserver.integration.onebot.model.event.MetaEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 元事件分发器（心跳/生命周期）
 */
@Slf4j
@Component
public class MetaEventDispatcher {

    @Autowired
    private GroupInfoSqliteService groupInfoSqliteService;
    @Autowired
    private FriendSqliteService friendSqliteService;

    public void onEvent(Bot bot, MetaEvent event) {
        if (MetaEventEnum.heartbeat.name().equals(event.getMetaEventType())) {
            // 心跳包
            Boolean good = event.getStatus() != null ? event.getStatus().getGood() : null;
            bot.updateHeartbeat(true, good, event.getTime());
            return;
        }
        if (!MetaEventEnum.lifecycle.name().equals(event.getMetaEventType())) {
            log.info("未知元事件类型: {}", event.getMetaEventType());
            return;
        }
        String subType = event.getSubType();
        if (SubTypeEnum.connect.name().equals(subType) || SubTypeEnum.enable.name().equals(subType)) {
            // 刚连接成功/启动时，qq客户端会发一条生命周期事件给bot
            bot.setId(event.getSelfId());
            log.info("收到QQ号连接：{} sessionId：{}", event.getSelfId(), bot.getSessionId());

            // 刷新bot信息
            bot.refreshSelfInfo();
            try {
                // 加载群信息
                groupInfoSqliteService.loadGroupInfo(bot);
                friendSqliteService.loadFriendInfo(bot);
            } catch (Exception e) {
                log.error("初始加载机器人群聊异常 bot：{}", event.getSelfId(), e);
            }
        } else if (SubTypeEnum.disable.name().equals(subType)) {
            // qq客户端已停止
            bot.markOffline();
            log.warn("机器人已停止运行，botId：{}", event.getSelfId());
        }
    }
}
