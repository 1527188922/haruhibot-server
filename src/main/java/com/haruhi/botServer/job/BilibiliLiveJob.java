package com.haruhi.botServer.job;

import cn.hutool.core.text.StrFormatter;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.haruhi.botServer.constant.BusinessModuleEnum;
import com.haruhi.botServer.constant.BilibiliSubscribeTypeEnum;
import com.haruhi.botServer.constant.event.MessageTypeEnum;
import com.haruhi.botServer.dto.bilibili.LiveStatusInfo;
import com.haruhi.botServer.dto.qqclient.MessageHolder;
import com.haruhi.botServer.entity.BilibiliSubscribeSqlite;
import com.haruhi.botServer.entity.FriendSqlite;
import com.haruhi.botServer.entity.GroupInfoSqlite;
import com.haruhi.botServer.job.schedule.AbstractJob;
import com.haruhi.botServer.service.BilibiliService;
import com.haruhi.botServer.service.BilibiliSubscribeSqliteService;
import com.haruhi.botServer.service.FriendSqliteService;
import com.haruhi.botServer.service.GroupInfoSqliteService;
import com.haruhi.botServer.utils.CommonUtil;
import com.haruhi.botServer.utils.DbLog;
import com.haruhi.botServer.utils.PushTargetUtil;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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

    @Autowired
    private GroupInfoSqliteService groupInfoSqliteService;

    @Autowired
    private FriendSqliteService friendSqliteService;

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

    /**
     * uid上次检测到的直播状态，没有记录时返回null
     */
    public static Integer getLastStatus(Long uid) {
        return Objects.isNull(uid) ? null : LAST_STATUS.get(uid);
    }

    /**
     * uid本次开播时间 单位秒，没有记录时返回null
     */
    public static Long getLiveStartTime(Long uid) {
        return Objects.isNull(uid) ? null : LIVE_START_TIME.get(uid);
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

        // 推送前需要判断机器人是否加入了对应的群、是否添加了好友，这里按self_id批量查询一次，避免每条订阅都查库
        Map<Long, Set<Long>> joinedGroupIds = loadJoinedGroupIds(subscribes);
        Map<Long, Set<Long>> joinedFriendIds = loadJoinedFriendIds(subscribes);

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
            // 每次请求到数据都刷新主播昵称与头像
            refreshLiveInfo(uid, info, entry.getValue());

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
                push(subscribe, message, joinedGroupIds, joinedFriendIds);
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

    /**
     * 主播昵称/头像/直播间id有变化时更新订阅表
     */
    private void refreshLiveInfo(Long uid, LiveStatusInfo info, List<BilibiliSubscribeSqlite> subscribes) {
        String uname = info.getUname();
        String face = normalizeFace(info.getFace());
        Long roomId = roomId(info);
        if (StringUtils.isBlank(uname) && StringUtils.isBlank(face) && Objects.isNull(roomId)) {
            return;
        }
        boolean changed = subscribes.stream().anyMatch(e ->
                (StringUtils.isNotBlank(uname) && !uname.equals(e.getUname()))
                        || (StringUtils.isNotBlank(face) && !face.equals(e.getFace()))
                        || (Objects.nonNull(roomId) && !roomId.equals(e.getRoomId())));
        if (!changed) {
            return;
        }
        bilibiliSubscribeSqliteService.refreshLiveInfo(uid, uname, face, roomId);
    }

    /**
     * 直播间id，取不到真实房间号时使用短号
     */
    private static Long roomId(LiveStatusInfo info) {
        if (Objects.nonNull(info.getRoomId()) && info.getRoomId() > 0) {
            return info.getRoomId();
        }
        if (Objects.nonNull(info.getShortId()) && info.getShortId() > 0) {
            return info.getShortId();
        }
        return null;
    }

    /**
     * 头像地址兼容协议相对路径
     */
    private static String normalizeFace(String face) {
        if (StringUtils.isBlank(face)) {
            return null;
        }
        return face.startsWith("//") ? "https:" + face : face;
    }

    /**
     * 按self_id查询机器人已加入的群
     */
    private Map<Long, Set<Long>> loadJoinedGroupIds(List<BilibiliSubscribeSqlite> subscribes) {
        List<Long> selfIds = subscribes.stream()
                .map(BilibiliSubscribeSqlite::getSelfId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(selfIds)) {
            return Collections.emptyMap();
        }
        List<GroupInfoSqlite> list = groupInfoSqliteService.list(new LambdaQueryWrapper<GroupInfoSqlite>()
                .select(GroupInfoSqlite::getSelfId, GroupInfoSqlite::getGroupId)
                .in(GroupInfoSqlite::getSelfId, selfIds));
        Map<Long, Set<Long>> map = new HashMap<>();
        for (GroupInfoSqlite e : list) {
            if (Objects.nonNull(e.getGroupId())) {
                map.computeIfAbsent(e.getSelfId(), k -> new HashSet<>()).add(e.getGroupId());
            }
        }
        return map;
    }

    /**
     * 按self_id查询机器人已添加的好友
     */
    private Map<Long, Set<Long>> loadJoinedFriendIds(List<BilibiliSubscribeSqlite> subscribes) {
        List<Long> selfIds = subscribes.stream()
                .map(BilibiliSubscribeSqlite::getSelfId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(selfIds)) {
            return Collections.emptyMap();
        }
        List<FriendSqlite> list = friendSqliteService.list(new LambdaQueryWrapper<FriendSqlite>()
                .select(FriendSqlite::getSelfId, FriendSqlite::getUserId)
                .in(FriendSqlite::getSelfId, selfIds));
        Map<Long, Set<Long>> map = new HashMap<>();
        for (FriendSqlite e : list) {
            if (Objects.nonNull(e.getUserId())) {
                map.computeIfAbsent(e.getSelfId(), k -> new HashSet<>()).add(e.getUserId());
            }
        }
        return map;
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
     * 发送前先判断机器人是否加入了该群、是否添加了该好友，未加入/未添加的直接跳过并打印警告
     */
    private void push(BilibiliSubscribeSqlite subscribe, List<MessageHolder> message,
                      Map<Long, Set<Long>> joinedGroupIds, Map<Long, Set<Long>> joinedFriendIds) {
        List<Long> groupIds = PushTargetUtil.parseIds(subscribe.getGroupIds());
        List<Long> friendIds = PushTargetUtil.parseIds(subscribe.getFriendIds());
        if (groupIds.isEmpty() && friendIds.isEmpty()) {
            return;
        }
        Long selfId = subscribe.getSelfId();
        Bot bot = BotContainer.getBotById(selfId);
        if (bot == null) {
            DbLog.warn(BusinessModuleEnum.JOB,"推送b站直播消息失败，机器人未连接 self_id:{} uid:{}", selfId, subscribe.getUid());
            return;
        }
        Set<Long> joinedGroups = joinedGroupIds.getOrDefault(selfId, Collections.emptySet());
        for (Long groupId : groupIds) {
            if (!joinedGroups.contains(groupId)) {
                DbLog.warn(BusinessModuleEnum.JOB,"推送b站直播消息失败，机器人未加入该群 self_id:{} group_id:{} uid:{}",
                        selfId, groupId, subscribe.getUid());
                continue;
            }
            bot.sendMessage(null, groupId, MessageTypeEnum.group.getType(), message);
        }
        Set<Long> joinedFriends = joinedFriendIds.getOrDefault(selfId, Collections.emptySet());
        for (Long friendId : friendIds) {
            if (!joinedFriends.contains(friendId)) {
                DbLog.warn(BusinessModuleEnum.JOB,"推送b站直播消息失败，机器人未添加该好友 self_id:{} friend_id:{} uid:{}",
                        selfId, friendId, subscribe.getUid());
                continue;
            }
            bot.sendMessage(friendId, null, MessageTypeEnum.privat.getType(), message);
        }
    }
}
