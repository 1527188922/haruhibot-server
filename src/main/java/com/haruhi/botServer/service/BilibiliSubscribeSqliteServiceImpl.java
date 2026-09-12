package com.haruhi.botServer.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.haruhi.botServer.constant.BilibiliSubscribeTypeEnum;
import com.haruhi.botServer.constant.PushTargetTypeEnum;
import com.haruhi.botServer.dto.bilibili.LiveStatusInfo;
import com.haruhi.botServer.entity.BilibiliSubscribeSqlite;
import com.haruhi.botServer.entity.FriendSqlite;
import com.haruhi.botServer.entity.GroupInfoSqlite;
import com.haruhi.botServer.exception.BusinessException;
import com.haruhi.botServer.job.BilibiliLiveJob;
import com.haruhi.botServer.mapper.BilibiliSubscribeSqliteMapper;
import com.haruhi.botServer.utils.CommonUtil;
import com.haruhi.botServer.utils.DateTimeUtil;
import com.haruhi.botServer.utils.PushTargetUtil;
import com.haruhi.botServer.vo.BilibiliSubscribeQueryReq;
import com.haruhi.botServer.vo.BilibiliSubscribeResp;
import com.haruhi.botServer.vo.PushTargetInfo;
import com.haruhi.botServer.vo.PushTargetQueryReq;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class BilibiliSubscribeSqliteServiceImpl extends ServiceImpl<BilibiliSubscribeSqliteMapper, BilibiliSubscribeSqlite>
        implements BilibiliSubscribeSqliteService {

    /**
     * 推送目标候选列表单次查询的最大数量
     */
    private static final int TARGET_CANDIDATE_MAX_LIMIT = 200;
    private static final int TARGET_CANDIDATE_DEFAULT_LIMIT = 50;

    @Autowired
    private GroupInfoSqliteService groupInfoSqliteService;

    @Autowired
    private FriendSqliteService friendSqliteService;

    @Override
    public List<BilibiliSubscribeSqlite> listEnabled(String subType) {
        if (StringUtils.isBlank(subType)) {
            return Collections.emptyList();
        }
        return this.list(new LambdaQueryWrapper<BilibiliSubscribeSqlite>()
                .eq(BilibiliSubscribeSqlite::getSubType, subType)
                .eq(BilibiliSubscribeSqlite::getEnableStatus, BilibiliSubscribeSqlite.ENABLE_STATUS_ENABLE));
    }

    @Override
    public IPage<BilibiliSubscribeResp> search(BilibiliSubscribeQueryReq request, boolean isPage) {
        LambdaQueryWrapper<BilibiliSubscribeSqlite> queryWrapper = new LambdaQueryWrapper<BilibiliSubscribeSqlite>()
                .eq(Objects.nonNull(request.getUid()), BilibiliSubscribeSqlite::getUid, request.getUid())
                .eq(Objects.nonNull(request.getSelfId()), BilibiliSubscribeSqlite::getSelfId, request.getSelfId())
                .like(StringUtils.isNotBlank(request.getUname()), BilibiliSubscribeSqlite::getUname, request.getUname())
                .eq(StringUtils.isNotBlank(request.getSubType()), BilibiliSubscribeSqlite::getSubType, request.getSubType())
                .eq(Objects.nonNull(request.getEnableStatus()), BilibiliSubscribeSqlite::getEnableStatus, request.getEnableStatus())
                .eq(Objects.nonNull(request.getOffNotify()), BilibiliSubscribeSqlite::getOffNotify, request.getOffNotify())
                .orderByDesc(BilibiliSubscribeSqlite::getId);

        IPage<BilibiliSubscribeSqlite> pageInfo;
        if (isPage) {
            pageInfo = this.page(new Page<>(request.getCurrentPage(), request.getPageSize()), queryWrapper);
        } else {
            pageInfo = new Page<>(request.getCurrentPage(), request.getPageSize());
            List<BilibiliSubscribeSqlite> list = this.list(queryWrapper);
            pageInfo.setRecords(list);
            pageInfo.setTotal(list.size());
        }

        IPage<BilibiliSubscribeResp> respPage = new Page<>(pageInfo.getCurrent(), pageInfo.getSize(), pageInfo.getTotal());
        List<BilibiliSubscribeSqlite> records = pageInfo.getRecords();
        if (CollectionUtils.isEmpty(records)) {
            respPage.setRecords(new ArrayList<>());
            return respPage;
        }

        // 批量查询推送目标(群/好友)的名称，避免逐条查询
        List<Long> groupIds = records.stream()
                .flatMap(e -> PushTargetUtil.parseIds(e.getGroupIds()).stream())
                .distinct()
                .collect(Collectors.toList());
        List<Long> friendIds = records.stream()
                .flatMap(e -> PushTargetUtil.parseIds(e.getFriendIds()).stream())
                .distinct()
                .collect(Collectors.toList());
        Map<Long, GroupInfoSqlite> groupMap = this.selectGroupMap(groupIds);
        Map<Long, FriendSqlite> friendMap = this.selectFriendMap(friendIds);

        respPage.setRecords(records.stream()
                .map(e -> this.toResp(e, groupMap, friendMap))
                .collect(Collectors.toList()));
        return respPage;
    }

    @Override
    public boolean saveSubscribe(BilibiliSubscribeSqlite entity) {
        String now = DateTimeUtil.dateTimeFormat(new Date(), DateTimeUtil.PatternEnum.yyyyMMddHHmmss);
        entity.setId(null);
        if (StringUtils.isBlank(entity.getSubType())) {
            entity.setSubType(BilibiliSubscribeTypeEnum.LIVE.getType());
        }
        if (Objects.isNull(entity.getEnableStatus())) {
            entity.setEnableStatus(BilibiliSubscribeSqlite.ENABLE_STATUS_ENABLE);
        }
        if (Objects.isNull(entity.getOffNotify())) {
            entity.setOffNotify(BilibiliSubscribeSqlite.OFF_NOTIFY_DISABLE);
        }
        entity.setGroupIds(PushTargetUtil.joinIds(PushTargetUtil.parseIds(entity.getGroupIds())));
        entity.setFriendIds(PushTargetUtil.joinIds(PushTargetUtil.parseIds(entity.getFriendIds())));
        entity.setCreateTime(now);
        entity.setUpdateTime(now);
        return this.save(entity);
    }

    @Override
    public boolean updateSubscribe(BilibiliSubscribeSqlite entity) {
        if (Objects.isNull(entity) || Objects.isNull(entity.getId())) {
            return false;
        }
        String now = DateTimeUtil.dateTimeFormat(new Date(), DateTimeUtil.PatternEnum.yyyyMMddHHmmss);
        return this.lambdaUpdate()
                .set(Objects.nonNull(entity.getUid()), BilibiliSubscribeSqlite::getUid, entity.getUid())
                .set(StringUtils.isNotBlank(entity.getSubType()), BilibiliSubscribeSqlite::getSubType, entity.getSubType())
                .set(Objects.nonNull(entity.getSelfId()), BilibiliSubscribeSqlite::getSelfId, entity.getSelfId())
                .set(Objects.nonNull(entity.getGroupIds()), BilibiliSubscribeSqlite::getGroupIds,
                        PushTargetUtil.joinIds(PushTargetUtil.parseIds(entity.getGroupIds())))
                .set(Objects.nonNull(entity.getFriendIds()), BilibiliSubscribeSqlite::getFriendIds,
                        PushTargetUtil.joinIds(PushTargetUtil.parseIds(entity.getFriendIds())))
                .set(Objects.nonNull(entity.getEnableStatus()), BilibiliSubscribeSqlite::getEnableStatus, entity.getEnableStatus())
                .set(Objects.nonNull(entity.getOffNotify()), BilibiliSubscribeSqlite::getOffNotify, entity.getOffNotify())
                .set(BilibiliSubscribeSqlite::getUpdateTime, now)
                .eq(BilibiliSubscribeSqlite::getId, entity.getId())
                .update();
    }

    @Override
    public boolean updateTargets(Long id, List<Long> groupIds, List<Long> friendIds) {
        if (Objects.isNull(id)) {
            return false;
        }
        String now = DateTimeUtil.dateTimeFormat(new Date(), DateTimeUtil.PatternEnum.yyyyMMddHHmmss);
        return this.lambdaUpdate()
                .set(BilibiliSubscribeSqlite::getGroupIds, PushTargetUtil.joinIds(groupIds))
                .set(BilibiliSubscribeSqlite::getFriendIds, PushTargetUtil.joinIds(friendIds))
                .set(BilibiliSubscribeSqlite::getUpdateTime, now)
                .eq(BilibiliSubscribeSqlite::getId, id)
                .update();
    }

    @Override
    public boolean refreshLiveInfo(Long uid, String uname, String face) {
        if (Objects.isNull(uid) || (StringUtils.isBlank(uname) && StringUtils.isBlank(face))) {
            return false;
        }
        String now = DateTimeUtil.dateTimeFormat(new Date(), DateTimeUtil.PatternEnum.yyyyMMddHHmmss);
        return this.lambdaUpdate()
                .set(StringUtils.isNotBlank(uname), BilibiliSubscribeSqlite::getUname, uname)
                .set(StringUtils.isNotBlank(face), BilibiliSubscribeSqlite::getFace, face)
                .set(BilibiliSubscribeSqlite::getUpdateTime, now)
                .eq(BilibiliSubscribeSqlite::getUid, uid)
                .eq(BilibiliSubscribeSqlite::getSubType, BilibiliSubscribeTypeEnum.LIVE.getType())
                .update();
    }

    @Override
    public List<PushTargetInfo> listTargetCandidates(PushTargetQueryReq request) {
        PushTargetTypeEnum typeEnum = PushTargetTypeEnum.getEnumByType(request.getType());
        if (Objects.isNull(typeEnum)) {
            throw new BusinessException("未知的推送目标类型:" + request.getType());
        }
        if (CollectionUtils.isNotEmpty(request.getIds())) {
            return this.listByIds(request, typeEnum);
        }

        int limit = request.getPageSize() > 0
                ? Math.min(request.getPageSize(), TARGET_CANDIDATE_MAX_LIMIT)
                : TARGET_CANDIDATE_DEFAULT_LIMIT;
        String keyword = request.getKeyword();
        if (typeEnum == PushTargetTypeEnum.GROUP) {
            LambdaQueryWrapper<GroupInfoSqlite> queryWrapper = new LambdaQueryWrapper<GroupInfoSqlite>()
                    .select(GroupInfoSqlite::getGroupId, GroupInfoSqlite::getGroupName, GroupInfoSqlite::getSelfId)
                    .eq(Objects.nonNull(request.getSelfId()), GroupInfoSqlite::getSelfId, request.getSelfId());
            if (StringUtils.isNotBlank(keyword)) {
                Long code = parseLongOrNull(keyword);
                queryWrapper.and(w -> {
                    w.like(GroupInfoSqlite::getGroupName, keyword);
                    if (Objects.nonNull(code)) {
                        w.or().eq(GroupInfoSqlite::getGroupId, code);
                    }
                });
            }
            queryWrapper.last("LIMIT " + limit);
            return distinctTargets(groupInfoSqliteService.list(queryWrapper), GroupInfoSqlite::getGroupId,
                    e -> toGroupTarget(e.getGroupId(), e));
        }

        LambdaQueryWrapper<FriendSqlite> queryWrapper = new LambdaQueryWrapper<FriendSqlite>()
                .select(FriendSqlite::getUserId, FriendSqlite::getNickname, FriendSqlite::getSelfId)
                .eq(Objects.nonNull(request.getSelfId()), FriendSqlite::getSelfId, request.getSelfId());
        if (StringUtils.isNotBlank(keyword)) {
            Long code = parseLongOrNull(keyword);
            queryWrapper.and(w -> {
                w.like(FriendSqlite::getNickname, keyword);
                if (Objects.nonNull(code)) {
                    w.or().eq(FriendSqlite::getUserId, code);
                }
            });
        }
        queryWrapper.orderByDesc(FriendSqlite::getLevel).last("LIMIT " + limit);
        return distinctTargets(friendSqliteService.list(queryWrapper), FriendSqlite::getUserId,
                e -> toFriendTarget(e.getUserId(), e));
    }

    @Override
    public boolean existsSubscribe(Long uid, Long selfId, String subType, Long excludeId) {
        return this.count(new LambdaQueryWrapper<BilibiliSubscribeSqlite>()
                .eq(Objects.nonNull(uid), BilibiliSubscribeSqlite::getUid, uid)
                .eq(Objects.nonNull(selfId), BilibiliSubscribeSqlite::getSelfId, selfId)
                .eq(StringUtils.isNotBlank(subType), BilibiliSubscribeSqlite::getSubType, subType)
                .ne(Objects.nonNull(excludeId), BilibiliSubscribeSqlite::getId, excludeId)) > 0;
    }

    /**
     * 查询指定id的推送目标信息
     */
    private List<PushTargetInfo> listByIds(PushTargetQueryReq request, PushTargetTypeEnum typeEnum) {
        List<Long> ids = request.getIds().stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(ids)) {
            return Collections.emptyList();
        }
        if (typeEnum == PushTargetTypeEnum.GROUP) {
            Map<Long, GroupInfoSqlite> map = this.selectGroupMap(ids);
            return ids.stream()
                    .map(id -> toGroupTarget(id, map.get(id)))
                    .collect(Collectors.toList());
        }
        Map<Long, FriendSqlite> map = this.selectFriendMap(ids);
        return ids.stream()
                .map(id -> toFriendTarget(id, map.get(id)))
                .collect(Collectors.toList());
    }

    /**
     * 按目标id去重，保留查询结果中的第一条
     */
    private <T> List<PushTargetInfo> distinctTargets(List<T> list, Function<T, Long> idGetter,
                                                     Function<T, PushTargetInfo> mapper) {
        Map<Long, PushTargetInfo> map = new LinkedHashMap<>();
        for (T e : list) {
            Long id = idGetter.apply(e);
            if (Objects.nonNull(id)) {
                map.putIfAbsent(id, mapper.apply(e));
            }
        }
        return new ArrayList<>(map.values());
    }

    private Map<Long, GroupInfoSqlite> selectGroupMap(List<Long> groupIds) {
        if (CollectionUtils.isEmpty(groupIds)) {
            return Collections.emptyMap();
        }
        List<GroupInfoSqlite> list = groupInfoSqliteService.list(new LambdaQueryWrapper<GroupInfoSqlite>()
                .select(GroupInfoSqlite::getGroupId, GroupInfoSqlite::getGroupName, GroupInfoSqlite::getSelfId)
                .in(GroupInfoSqlite::getGroupId, groupIds));
        Map<Long, GroupInfoSqlite> map = new HashMap<>();
        list.forEach(e -> map.putIfAbsent(e.getGroupId(), e));
        return map;
    }

    private Map<Long, FriendSqlite> selectFriendMap(List<Long> friendIds) {
        if (CollectionUtils.isEmpty(friendIds)) {
            return Collections.emptyMap();
        }
        List<FriendSqlite> list = friendSqliteService.list(new LambdaQueryWrapper<FriendSqlite>()
                .select(FriendSqlite::getUserId, FriendSqlite::getNickname, FriendSqlite::getSelfId)
                .in(FriendSqlite::getUserId, friendIds));
        Map<Long, FriendSqlite> map = new HashMap<>();
        list.forEach(e -> map.putIfAbsent(e.getUserId(), e));
        return map;
    }

    private BilibiliSubscribeResp toResp(BilibiliSubscribeSqlite entity,
                                         Map<Long, GroupInfoSqlite> groupMap,
                                         Map<Long, FriendSqlite> friendMap) {
        BilibiliSubscribeResp resp = new BilibiliSubscribeResp();
        BeanUtils.copyProperties(entity, resp);
        resp.setSelfAvatarUrl(CommonUtil.getAvatarUrl(entity.getSelfId(), false));
        resp.setGroupInfos(PushTargetUtil.parseIds(entity.getGroupIds()).stream()
                .map(id -> toGroupTarget(id, groupMap.get(id)))
                .collect(Collectors.toList()));
        resp.setFriendInfos(PushTargetUtil.parseIds(entity.getFriendIds()).stream()
                .map(id -> toFriendTarget(id, friendMap.get(id)))
                .collect(Collectors.toList()));
        this.fillLiveStatus(resp);
        return resp;
    }

    /**
     * 填充内存中的直播状态(开播中才有值)
     */
    private void fillLiveStatus(BilibiliSubscribeResp resp) {
        Long uid = resp.getUid();
        if (Objects.isNull(uid)) {
            return;
        }
        Integer lastStatus = BilibiliLiveJob.getLastStatus(uid);
        if (Objects.isNull(lastStatus) || lastStatus != LiveStatusInfo.STATUS_LIVE) {
            return;
        }
        resp.setLiving(true);
        Long startTime = BilibiliLiveJob.getLiveStartTime(uid);
        if (Objects.nonNull(startTime) && startTime > 0) {
            resp.setLiveStartTime(startTime);
            resp.setLiveStartTimeText(DateTimeUtil.dateTimeFormat(startTime * 1000, DateTimeUtil.PatternEnum.yyyyMMddHHmmss));
            long seconds = Math.max(0, System.currentTimeMillis() / 1000 - startTime);
            resp.setLiveDuration(CommonUtil.formatDuration(seconds, TimeUnit.SECONDS));
        }
    }

    private PushTargetInfo toGroupTarget(Long groupId, GroupInfoSqlite groupInfo) {
        return new PushTargetInfo(groupId,
                Objects.isNull(groupInfo) ? null : groupInfo.getGroupName(),
                CommonUtil.getGroupAvatarUrl(groupId, false),
                Objects.nonNull(groupInfo));
    }

    private PushTargetInfo toFriendTarget(Long userId, FriendSqlite friend) {
        return new PushTargetInfo(userId,
                Objects.isNull(friend) ? null : friend.getNickname(),
                CommonUtil.getAvatarUrl(userId, false),
                Objects.nonNull(friend));
    }

    private static Long parseLongOrNull(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
