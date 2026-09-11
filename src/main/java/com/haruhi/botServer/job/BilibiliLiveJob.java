package com.haruhi.botServer.job;

import cn.hutool.core.text.StrFormatter;
import com.haruhi.botServer.constant.BusinessModuleEnum;
import com.haruhi.botServer.constant.BilibiliSubscribeTypeEnum;
import com.haruhi.botServer.constant.event.MessageTypeEnum;
import com.haruhi.botServer.dto.bilibili.LiveStatusInfo;
import com.haruhi.botServer.dto.qqclient.MessageHolder;
import com.haruhi.botServer.entity.BilibiliSubscribeSqlite;
import com.haruhi.botServer.job.schedule.AbstractJob;
import com.haruhi.botServer.service.BilibiliService;
import com.haruhi.botServer.service.BilibiliSubscribeSqliteService;
import com.haruhi.botServer.utils.CommonUtil;
import com.haruhi.botServer.utils.DbLog;
import com.haruhi.botServer.ws.Bot;
import com.haruhi.botServer.ws.BotContainer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * b站主播开播/下播推送
 * 订阅数据来源 t_subscribe，sub_type = live
 * 状态检测逻辑：定时拉取订阅主播的直播间状态，与上一次检测的状态比对，状态发生变化才推送
 * 首次检测到某主播（服务重启、新增订阅）只记录状态不推送，避免重复推送正在直播的主播
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "job.bilibiliLive.enable", havingValue = "1")
public class BilibiliLiveJob extends AbstractJob {

    @Value("${job.bilibiliLive.cron}")
    private String cron;

    @Autowired
    private BilibiliSubscribeSqliteService bilibiliSubscribeSqliteService;

    @Autowired
    private BilibiliService bilibiliService;

    /**
     * uid -> 上次检测到的直播状态 0:未开播 1:直播中(轮播视为未开播)
     * quartz每次执行都是新的job实例，所以状态需要放在静态变量中
     */
    private static final Map<Long, Integer> LAST_STATUS = new ConcurrentHashMap<>();
    /**
     * uid -> 本次开播时间 单位秒 用于计算下播时的直播时长
     */
    private static final Map<Long, Long> LIVE_START_TIME = new ConcurrentHashMap<>();

    @Override
    public String cronExpression() {
        return cron;
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        List<BilibiliSubscribeSqlite> subscribes = bilibiliSubscribeSqliteService.listEnabled(BilibiliSubscribeTypeEnum.LIVE.getType());
        if (CollectionUtils.isEmpty(subscribes)) {
            return;
        }

        // 同一个主播可能被订阅了多次(多个群/多个机器人)，按uid分组，接口只需要查一次
        Map<Long, List<BilibiliSubscribeSqlite>> uidSubscribes = subscribes.stream()
                .filter(e -> Objects.nonNull(e.getUid()))
                .collect(Collectors.groupingBy(BilibiliSubscribeSqlite::getUid));
        if (uidSubscribes.isEmpty()) {
            return;
        }

        Map<Long, LiveStatusInfo> statusMap = bilibiliService.getLiveStatusInfoByUids(uidSubscribes.keySet());
        if (statusMap.isEmpty()) {
            log.error("获取b站直播状态失败，本次不检测");
            return;
        }

        // 清理已取消订阅的主播的状态缓存，避免内存泄漏，也保证重新订阅后首次不推送
        LAST_STATUS.keySet().removeIf(uid -> !uidSubscribes.containsKey(uid));
        LIVE_START_TIME.keySet().removeIf(uid -> !uidSubscribes.containsKey(uid));

        int living = 0;
        for (Map.Entry<Long, List<BilibiliSubscribeSqlite>> entry : uidSubscribes.entrySet()) {
            Long uid = entry.getKey();
            LiveStatusInfo info = statusMap.get(uid);
            if (info == null) {
                // 主播uid无效或该批次请求失败，不更新状态，避免误判为下播
                log.debug("b站直播状态接口未返回该主播的状态 uid:{}", uid);
                continue;
            }
            int newStatus = info.isLiving() ? LiveStatusInfo.STATUS_LIVE : LiveStatusInfo.STATUS_OFF;
            if (newStatus == LiveStatusInfo.STATUS_LIVE) {
                living++;
            }

            Integer oldStatus = LAST_STATUS.get(uid);
            if (oldStatus == null) {
                // 首次检测到该主播，只记录状态
                recordStatus(uid, info, newStatus);
                continue;
            }
            if (oldStatus == newStatus) {
                continue;
            }
            recordStatus(uid, info, newStatus);

            boolean live = newStatus == LiveStatusInfo.STATUS_LIVE;
            DbLog.info(BusinessModuleEnum.JOB, "检测到b站主播{}：{}（{}）", live ? "开播" : "下播", info.getUname(), uid);
            List<MessageHolder> message = live ? buildLiveMessage(info) : buildOffMessage(uid, info);
            for (BilibiliSubscribeSqlite subscribe : entry.getValue()) {
                if (!live && !isOffNotify(subscribe)) {
                    continue;
                }
                push(subscribe, message);
            }
        }
        log.debug("b站直播状态检测完成，目前开播{}人，总共{}人", living, uidSubscribes.size());
    }

    private void recordStatus(Long uid, LiveStatusInfo info, int newStatus) {
        LAST_STATUS.put(uid, newStatus);
        if (newStatus == LiveStatusInfo.STATUS_LIVE && info.getLiveTime() != null && info.getLiveTime() > 0) {
            LIVE_START_TIME.put(uid, info.getLiveTime());
        } else {
            LIVE_START_TIME.remove(uid);
        }
    }

    private boolean isOffNotify(BilibiliSubscribeSqlite subscribe) {
        return subscribe.getOffNotify() != null && subscribe.getOffNotify() == BilibiliSubscribeSqlite.OFF_NOTIFY_ENABLE;
    }

    /**
     * 开播消息：主播名 分区 标题 封面 直播间地址
     */
    private List<MessageHolder> buildLiveMessage(LiveStatusInfo info) {
        List<MessageHolder> message = new ArrayList<>();
        message.addAll(MessageHolder.instanceText(StrFormatter.format("{} 开播啦！\n分区：{}\n标题：{}",
                info.getUname(), info.areaFullName(), info.getTitle())));
        if (StringUtils.isNotBlank(info.cover())) {
            message.add(MessageHolder.instanceImage(info.cover()));
        }
        if (StringUtils.isNotBlank(info.roomUrl())) {
            message.addAll(MessageHolder.instanceText(info.roomUrl()));
        }
        return message;
    }

    /**
     * 下播消息：主播名 直播时长
     */
    private List<MessageHolder> buildOffMessage(Long uid, LiveStatusInfo info) {
        Long startTime = LIVE_START_TIME.remove(uid);
        String duration = "。";
        if (startTime != null && startTime > 0) {
            long seconds = Math.max(0, System.currentTimeMillis() / 1000 - startTime);
            duration = StrFormatter.format("\n本次直播时长 {}。", CommonUtil.formatDuration(seconds, TimeUnit.SECONDS));
        }
        return MessageHolder.instanceText(StrFormatter.format("{} 下播了{}", info.getUname(), duration));
    }

    /**
     * 推送到订阅配置的群和好友
     */
    private void push(BilibiliSubscribeSqlite subscribe, List<MessageHolder> message) {
        List<Long> groupIds = parseIds(subscribe.getGroupIds());
        List<Long> friendIds = parseIds(subscribe.getFriendIds());
        if (groupIds.isEmpty() && friendIds.isEmpty()) {
            return;
        }
        Bot bot = BotContainer.getBotById(subscribe.getSelfId());
        if (bot == null) {
            DbLog.warn(BusinessModuleEnum.JOB,"推送b站直播消息失败，机器人未连接 self_id:{} uid:{}", subscribe.getSelfId(), subscribe.getUid());
            return;
        }
        for (Long groupId : groupIds) {
            bot.sendMessage(null, groupId, MessageTypeEnum.group.getType(), message);
        }
        for (Long friendId : friendIds) {
            bot.sendMessage(friendId, null, MessageTypeEnum.privat.getType(), message);
        }
    }

    /**
     * 解析逗号分割的群号/qq号，兼容中文逗号与空白符
     */
    private static List<Long> parseIds(String ids) {
        if (StringUtils.isBlank(ids)) {
            return Collections.emptyList();
        }
        return Arrays.stream(ids.split("[,，\\s]+"))
                .filter(StringUtils::isNotBlank)
                .map(String::trim)
                .map(id -> {
                    try {
                        return Long.parseLong(id);
                    } catch (NumberFormatException e) {
                        log.error("解析订阅推送目标异常 id:{}", id);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }
}
