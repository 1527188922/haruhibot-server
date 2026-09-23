package com.haruhi.botserver.features.bilibili.handler;

import cn.hutool.core.text.StrFormatter;
import com.haruhi.botserver.bot.handler.HandlerWeightEnum;
import com.haruhi.botserver.shared.constant.RegexEnum;
import com.haruhi.botserver.integration.onebot.model.Message;
import com.haruhi.botserver.features.bilibili.persistence.entity.BilibiliSubscribeSqlite;
import com.haruhi.botserver.features.notification.support.PushTargetUtil;
import com.haruhi.botserver.infrastructure.concurrent.ThreadPoolUtil;
import com.haruhi.botserver.bot.session.Bot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 取消b站直播订阅
 * 指令：取消b站订阅{uid} | 取消订阅up{uid} | 取消订阅b站up{uid} | 取消订阅b站主播{uid} (不区分大小写)
 * 群聊取消本群的推送，私聊取消发给自己的推送
 * 取消后没有任何推送目标时不会删除订阅记录，只清空推送目标
 */
@Slf4j
@Component
public class BilibiliLiveUnsubscribeHandler extends AbstractBilibiliSubscribeHandler {

    private static final Pattern PATTERN = Pattern.compile(RegexEnum.BILIBILI_LIVE_UNSUBSCRIBE.getValue());

    @Override
    public int weight() {
        return HandlerWeightEnum.W_580.getWeight();
    }

    @Override
    public String funName() {
        return HandlerWeightEnum.W_580.getName();
    }

    @Override
    public boolean onMessage(Bot bot, Message message) {
        Long uid = this.parseUid(message, PATTERN);
        if (Objects.isNull(uid)) {
            return false;
        }
        ThreadPoolUtil.getHandleCommandPool().execute(() -> {
            try {
                this.unsubscribe(bot, message, uid);
            } catch (Exception e) {
                log.error("取消b站订阅异常 uid:{}", uid, e);
                this.reply(bot, message, StrFormatter.format("取消b站订阅异常：{}", e.getMessage()));
            }
        });
        return true;
    }

    private void unsubscribe(Bot bot, Message message, Long uid) {
        Long selfId = this.selfId(bot, message);
        boolean groupMsg = message.isGroupMsg();
        Long targetId = this.targetId(message);

        BilibiliSubscribeSqlite subscribe = this.findSubscribe(uid, selfId);
        if (Objects.isNull(subscribe)) {
            this.reply(bot, message, StrFormatter.format("{}未订阅该b站主播：uid:{}", this.selfLabel(message), uid));
            return;
        }

        List<Long> groupIds = new ArrayList<>(PushTargetUtil.parseIds(subscribe.getGroupIds()));
        List<Long> friendIds = new ArrayList<>(PushTargetUtil.parseIds(subscribe.getFriendIds()));
        boolean removed = groupMsg ? groupIds.remove(targetId) : friendIds.remove(targetId);
        String name = this.displayName(uid, subscribe.getUname());
        if (!removed) {
            this.reply(bot, message, StrFormatter.format("{}未订阅该b站主播：{}", this.selfLabel(message), name));
            return;
        }

        if (groupIds.isEmpty() && friendIds.isEmpty()) {
            // 已无任何推送目标，保留订阅记录(不删除)，只清空推送目标
            bilibiliSubscribeSqliteService.updateTargets(subscribe.getId(), groupIds, friendIds);
            this.reply(bot, message, StrFormatter.format("已取消订阅：{}\n该订阅已无推送目标", name));
            return;
        }
        bilibiliSubscribeSqliteService.updateTargets(subscribe.getId(), groupIds, friendIds);
        this.reply(bot, message, StrFormatter.format("已取消订阅：{}", name));
    }
}
