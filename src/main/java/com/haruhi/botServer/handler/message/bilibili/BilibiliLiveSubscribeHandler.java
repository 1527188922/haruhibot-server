package com.haruhi.botServer.handler.message.bilibili;

import cn.hutool.core.text.StrFormatter;
import com.haruhi.botServer.constant.BusinessModuleEnum;
import com.haruhi.botServer.constant.HandlerWeightEnum;
import com.haruhi.botServer.constant.RegexEnum;
import com.haruhi.botServer.constant.BilibiliSubscribeTypeEnum;
import com.haruhi.botServer.dto.bilibili.LiveStatusInfo;
import com.haruhi.botServer.dto.qqclient.Message;
import com.haruhi.botServer.entity.BilibiliSubscribeSqlite;
import com.haruhi.botServer.job.BilibiliLiveJob;
import com.haruhi.botServer.service.BilibiliService;
import com.haruhi.botServer.utils.DbLog;
import com.haruhi.botServer.utils.PushTargetUtil;
import com.haruhi.botServer.utils.ThreadPoolUtil;
import com.haruhi.botServer.ws.Bot;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * b站直播订阅
 * 指令：b站订阅{uid} | 订阅b站{uid} | 订阅b站up{uid} | 订阅b站直播{uid} (不区分大小写)
 * 群聊订阅到本群，私聊订阅给发消息的人
 * 同一个主播已订阅时只追加推送目标，不重复创建订阅
 */
@Slf4j
@Component
public class BilibiliLiveSubscribeHandler extends AbstractBilibiliSubscribeHandler {

    private static final Pattern PATTERN = Pattern.compile(RegexEnum.BILIBILI_LIVE_SUBSCRIBE.getValue());

    @Autowired
    private BilibiliService bilibiliService;

    /**
     * job.bilibiliLive.enable = 1 时该bean才会被创建，为null说明定时任务未开启，订阅后不会推送
     */
    @Autowired(required = false)
    private BilibiliLiveJob bilibiliLiveJob;

    @Override
    public int weight() {
        return HandlerWeightEnum.W_581.getWeight();
    }

    @Override
    public String funName() {
        return HandlerWeightEnum.W_581.getName();
    }

    @Override
    public boolean onMessage(Bot bot, Message message) {
        Long uid = this.parseUid(message, PATTERN);
        if (Objects.isNull(uid)) {
            return false;
        }
        ThreadPoolUtil.getHandleCommandPool().execute(() -> {
            try {
                this.subscribe(bot, message, uid);
            } catch (Exception e) {
                DbLog.error(BusinessModuleEnum.BILIBILI,"b站订阅异常 uid:{}", uid, e);
                this.reply(bot, message, StrFormatter.format("b站订阅异常：{}", e.getMessage()));
            }
        });
        return true;
    }

    private void subscribe(Bot bot, Message message, Long uid) {
        Long selfId = this.selfId(bot, message);
        boolean groupMsg = message.isGroupMsg();
        Long targetId = this.targetId(message);

        BilibiliSubscribeSqlite subscribe = this.findSubscribe(uid, selfId);
        List<Long> groupIds = new ArrayList<>();
        List<Long> friendIds = new ArrayList<>();
        if (Objects.nonNull(subscribe)) {
            groupIds.addAll(PushTargetUtil.parseIds(subscribe.getGroupIds()));
            friendIds.addAll(PushTargetUtil.parseIds(subscribe.getFriendIds()));
            if ((groupMsg && groupIds.contains(targetId)) || (!groupMsg && friendIds.contains(targetId))) {
                this.reply(bot, message, StrFormatter.format("{}已订阅该b站主播：{}", this.selfLabel(message),
                        this.displayName(uid, subscribe.getUname())) + disabledTip(subscribe));
                return;
            }
        }

        // 查询主播信息，查询不到也允许订阅，定时任务执行时会再刷新
        LiveStatusInfo info = this.queryLiveInfo(uid);

        if (Objects.isNull(subscribe)) {
            subscribe = new BilibiliSubscribeSqlite();
            subscribe.setUid(uid);
            subscribe.setSubType(BilibiliSubscribeTypeEnum.LIVE.getType());
            subscribe.setSelfId(selfId);
            subscribe.setEnableStatus(BilibiliSubscribeSqlite.ENABLE_STATUS_ENABLE);
            subscribe.setOffNotify(BilibiliSubscribeSqlite.OFF_NOTIFY_DISABLE);
            if (Objects.nonNull(info)) {
                subscribe.setUname(info.getUname());
                subscribe.setFace(info.faceUrl());
                subscribe.setRoomId(info.liveRoomId());
            }
            if (groupMsg) {
                groupIds.add(targetId);
            } else {
                friendIds.add(targetId);
            }
            subscribe.setGroupIds(PushTargetUtil.joinIds(groupIds));
            subscribe.setFriendIds(PushTargetUtil.joinIds(friendIds));
            bilibiliSubscribeSqliteService.saveSubscribe(subscribe);
        } else {
            if (groupMsg) {
                groupIds.add(targetId);
            } else {
                friendIds.add(targetId);
            }
            bilibiliSubscribeSqliteService.updateTargets(subscribe.getId(), groupIds, friendIds);
        }

        String uname = Objects.nonNull(info) && StringUtils.isNotBlank(info.getUname())
                ? info.getUname() : subscribe.getUname();
        StringBuilder text = new StringBuilder("订阅成功：").append(this.displayName(uid, uname));
        text.append("\n开播/下播消息将").append(groupMsg ? "推送到本群" : "私聊推送给您");
        if (Objects.nonNull(info) && info.isLiving()) {
            // 订阅时主播正在直播，直接提示
            text.append("\nup正在直播中哦~");
        }
        if (StringUtils.isBlank(uname)) {
            text.append("\n未查询到主播信息，请确认uid是否正确");
        }
        text.append(disabledTip(subscribe));
        text.append(jobDisabledTip());
        this.reply(bot, message, text.toString());
    }

    /**
     * 订阅已存在但被禁用时的提示，启用状态由管理界面控制，这里只提示不修改
     */
    private static String disabledTip(BilibiliSubscribeSqlite subscribe) {
        if (Objects.isNull(subscribe) || Objects.isNull(subscribe.getEnableStatus())) {
            return "";
        }
        return subscribe.getEnableStatus() == BilibiliSubscribeSqlite.ENABLE_STATUS_DISABLE
                ? "\n注意：该订阅当前为禁用状态，需在管理界面启用后才会推送" : "";
    }

    /**
     * 定时任务未开启时的提示
     */
    private String jobDisabledTip() {
        return Objects.isNull(bilibiliLiveJob)
                ? "\n注意：b站直播推送定时任务未开启(job.bilibiliLive.enable)，暂时不会推送消息" : "";
    }

    /**
     * 请求b站直播状态接口获取主播昵称/头像/直播间id
     */
    private LiveStatusInfo queryLiveInfo(Long uid) {
        try {
            Map<Long, LiveStatusInfo> map = bilibiliService.getLiveStatusInfoByUids(Collections.singletonList(uid));
            if (MapUtils.isEmpty(map)) {
                return null;
            }
            return map.get(uid);
        } catch (Exception e) {
            log.error("查询b站主播信息异常 uid:{}", uid, e);
            return null;
        }
    }
}
