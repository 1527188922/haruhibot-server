package com.haruhi.botserver.bot.dispatch;

import com.haruhi.botserver.bot.handler.IMetaEventHandler;
import com.haruhi.botserver.bot.session.Bot;
import com.haruhi.botserver.features.contacts.service.FriendSqliteService;
import com.haruhi.botserver.features.contacts.service.GroupInfoSqliteService;
import com.haruhi.botserver.integration.onebot.model.MetaEventEnum;
import com.haruhi.botserver.integration.onebot.model.SubTypeEnum;
import com.haruhi.botserver.integration.onebot.model.event.MetaEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 元事件分发器（心跳/生命周期）
 * 先做内置的状态前置处理（在线状态、初始化加载等），再交给所有 IMetaEventHandler 处理
 */
@Slf4j
@Component
public class MetaEventDispatcher {

    private final List<IMetaEventHandler> container = new ArrayList<>();

    @Autowired
    private GroupInfoSqliteService groupInfoSqliteService;
    @Autowired
    private FriendSqliteService friendSqliteService;

    public MetaEventDispatcher(Map<String, IMetaEventHandler> handlerMap) {
        if (!CollectionUtils.isEmpty(handlerMap)) {
            container.addAll(handlerMap.values());
            log.info("加载了{}个元事件处理类", container.size());
        }
    }

    public void onEvent(Bot bot, MetaEvent event) {
        if (event == null) {
            return;
        }
        String metaEventType = event.getMetaEventType();
        if (MetaEventEnum.heartbeat.name().equals(metaEventType)) {
            // 心跳包，前置更新在线状态
            Boolean good = event.getStatus() != null ? event.getStatus().getGood() : null;
            bot.updateHeartbeat(true, good, event.getTime());
        } else if (MetaEventEnum.lifecycle.name().equals(metaEventType)) {
            handleLifecycle(bot, event);
        } else {
            log.info("未知元事件类型: {}", metaEventType);
        }

        // 前置处理完成后，交给处理器做后续逻辑
        for (IMetaEventHandler handler : container) {
            try {
                if (handler.support(event)) {
                    handler.onMetaEvent(bot, event);
                }
            } catch (Exception e) {
                log.error("处理元事件异常 metaEventType:{} subType:{} handler:{}",
                        metaEventType, event.getSubType(), handler.getClass().getSimpleName(), e);
            }
        }
    }

    private void handleLifecycle(Bot bot, MetaEvent event) {
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
