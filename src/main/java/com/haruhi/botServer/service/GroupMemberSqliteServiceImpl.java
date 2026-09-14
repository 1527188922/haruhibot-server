package com.haruhi.botServer.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.haruhi.botServer.dto.qqclient.GroupMember;
import com.haruhi.botServer.dto.qqclient.SyncResponse;
import com.haruhi.botServer.entity.GroupInfoSqlite;
import com.haruhi.botServer.entity.GroupMemberSqlite;
import com.haruhi.botServer.exception.BusinessException;
import com.haruhi.botServer.mapper.GroupMemberSqliteMapper;
import com.haruhi.botServer.utils.CommonUtil;
import com.haruhi.botServer.utils.DateTimeUtil;
import com.haruhi.botServer.vo.GroupMemberQueryReq;
import com.haruhi.botServer.vo.GroupMemberRefreshResp;
import com.haruhi.botServer.ws.Bot;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class GroupMemberSqliteServiceImpl extends ServiceImpl<GroupMemberSqliteMapper, GroupMemberSqlite> implements GroupMemberSqliteService {

    @Autowired
    private GroupInfoSqliteService groupInfoSqliteService;

    /**
     * 从qq客户端拉取群成员并入库
     * 本次数据中不存在的群员标记为离群（只标记不删除）
     */
    @Override
    public GroupMemberRefreshResp refreshGroupMember(Bot bot, Long groupId, long timeout) {
        if (Objects.isNull(bot) || Objects.isNull(groupId)) {
            throw new BusinessException("机器人与群号不能为空");
        }
        Long selfId = bot.getId();
        GroupMemberRefreshResp resp = new GroupMemberRefreshResp();
        resp.setSelfId(selfId);
        resp.setGroupId(groupId);

        SyncResponse<List<GroupMember>> syncResponse = bot.getGroupMemberList(groupId, timeout);
        if (!syncResponse.isSuccess()) {
            String msg = StringUtils.isNotBlank(syncResponse.getMessage()) ? syncResponse.getMessage() : syncResponse.getWording();
            throw new BusinessException("获取群成员失败：" + (StringUtils.isNotBlank(msg) ? msg : "无响应"));
        }
        List<GroupMember> data = syncResponse.getData();
        if (CollectionUtils.isEmpty(data)) {
            // 未获取到任何群成员时不做离群标记，避免把全部群员误标为离群
            log.warn("刷新群成员未获取到数据, selfId:{} groupId:{}", selfId, groupId);
            return resp;
        }

        String now = DateTimeUtil.dateTimeFormat(new Date(), DateTimeUtil.PatternEnum.yyyyMMddHHmmss);
        Map<Long, GroupMemberSqlite> dbMap = this.list(new LambdaQueryWrapper<GroupMemberSqlite>()
                        .eq(GroupMemberSqlite::getSelfId, selfId)
                        .eq(GroupMemberSqlite::getGroupId, groupId))
                .stream()
                .filter(e -> Objects.nonNull(e.getUserId()))
                .collect(Collectors.toMap(GroupMemberSqlite::getUserId, e -> e, (a, b) -> a));

        Set<Long> fetchedUserIds = new HashSet<>(data.size());
        List<GroupMemberSqlite> needAdd = new ArrayList<>();
        List<GroupMemberSqlite> needUpdate = new ArrayList<>();
        List<GroupMemberSqlite> rejoinList = new ArrayList<>();

        for (GroupMember member : data) {
            if (Objects.isNull(member) || Objects.isNull(member.getUserId()) || !fetchedUserIds.add(member.getUserId())) {
                continue;
            }
            GroupMemberSqlite entity = toEntity(member, selfId, groupId);
            entity.setModifyTime(now);
            GroupMemberSqlite dbData = dbMap.get(member.getUserId());
            if (Objects.isNull(dbData)) {
                // 新成员
                entity.setLeftFlag(0);
                entity.setCreateTime(now);
                needAdd.add(entity);
                resp.getAddedList().add(entity);
                continue;
            }
            entity.setId(dbData.getId());
            entity.setCreateTime(dbData.getCreateTime());
            if (Integer.valueOf(1).equals(dbData.getLeftFlag())) {
                // 之前标记为离群，本次又出现在群成员列表中
                GroupMemberSqlite rejoin = new GroupMemberSqlite();
                BeanUtils.copyProperties(entity, rejoin);
                rejoin.setLeftFlag(0);
                rejoinList.add(rejoin);
            }
            entity.setLeftFlag(0);
            needUpdate.add(entity);
        }

        // 数据库中在群、但本次未获取到的群员 = 已离群
        List<GroupMemberSqlite> leftList = dbMap.values().stream()
                .filter(e -> !Integer.valueOf(1).equals(e.getLeftFlag()))
                .filter(e -> !fetchedUserIds.contains(e.getUserId()))
                .collect(Collectors.toList());
        leftList.forEach(e -> {
            e.setLeftFlag(1);
            e.setModifyTime(now);
        });

        if (CollectionUtils.isNotEmpty(needAdd)) {
            this.saveBatch(needAdd);
        }
        if (CollectionUtils.isNotEmpty(needUpdate)) {
            this.updateBatchById(needUpdate);
        }
        if (CollectionUtils.isNotEmpty(leftList)) {
            this.updateBatchById(leftList);
        }

        resp.setMemberCount(fetchedUserIds.size());
        resp.setLeftList(fillAvatar(leftList));
        resp.setRejoinList(fillAvatar(rejoinList));
        resp.setAddedList(fillAvatar(resp.getAddedList()));
        log.info("刷新群成员完成, selfId:{} groupId:{} 本次成员数:{} 新增:{} 离群:{} 回归:{}",
                selfId, groupId, resp.getMemberCount(), needAdd.size(), leftList.size(), rejoinList.size());
        return resp;
    }

    @Override
    public IPage<GroupMemberSqlite> search(GroupMemberQueryReq request) {
        if (Objects.isNull(request)) {
            request = new GroupMemberQueryReq();
        }
        LambdaQueryWrapper<GroupMemberSqlite> queryWrapper = new LambdaQueryWrapper<GroupMemberSqlite>()
                .eq(Objects.nonNull(request.getSelfId()), GroupMemberSqlite::getSelfId, request.getSelfId())
                .eq(Objects.nonNull(request.getGroupId()), GroupMemberSqlite::getGroupId, request.getGroupId())
                .eq(Objects.nonNull(request.getUserId()), GroupMemberSqlite::getUserId, request.getUserId())
                .like(StringUtils.isNotBlank(request.getNickname()), GroupMemberSqlite::getNickname, request.getNickname())
                .like(StringUtils.isNotBlank(request.getCard()), GroupMemberSqlite::getCard, request.getCard())
                .eq(Objects.nonNull(request.getLeftFlag()), GroupMemberSqlite::getLeftFlag, request.getLeftFlag())
                .last("""
                        ORDER BY group_id asc,
                        CASE role WHEN 'owner' THEN 1 WHEN 'admin' THEN 2 ELSE 3 END asc,
                        left_flag asc,
                        last_sent_time desc,
                        user_id ASC
                        """);
        IPage<GroupMemberSqlite> pageInfo = this.page(new Page<>(request.getCurrentPage(), request.getPageSize()), queryWrapper);

        List<GroupMemberSqlite> records = pageInfo.getRecords();
        if (CollectionUtils.isNotEmpty(records)) {
            List<Long> groupIds = records.stream()
                    .map(GroupMemberSqlite::getGroupId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());
            Map<Long, List<GroupInfoSqlite>> groupMap = groupInfoSqliteService.selectMapByGroupIds(groupIds);
            records.forEach(e -> {
                e.setUserAvatarUrl(CommonUtil.getAvatarUrl(e.getUserId(), false));
                e.setGroupAvatarUrl(CommonUtil.getGroupAvatarUrl(e.getGroupId(), false));
                e.setSelfAvatarUrl(CommonUtil.getAvatarUrl(e.getSelfId(), false));
                e.setGroupName(findGroupName(groupMap.get(e.getGroupId()), e.getSelfId()));
            });
        }
        return pageInfo;
    }

    private String findGroupName(List<GroupInfoSqlite> groupList, Long selfId) {
        if (CollectionUtils.isEmpty(groupList)) {
            return null;
        }
        return groupList.stream()
                .filter(e -> Objects.equals(e.getSelfId(), selfId))
                .map(GroupInfoSqlite::getGroupName)
                .findFirst()
                .orElseGet(() -> groupList.stream()
                        .map(GroupInfoSqlite::getGroupName)
                        .filter(StringUtils::isNotBlank)
                        .findFirst()
                        .orElse(null));
    }

    private GroupMemberSqlite toEntity(GroupMember member, Long selfId, Long groupId) {
        GroupMemberSqlite entity = new GroupMemberSqlite();
        BeanUtils.copyProperties(member, entity);
        entity.setSelfId(selfId);
        entity.setGroupId(groupId);
        return entity;
    }

    private List<GroupMemberSqlite> fillAvatar(Collection<GroupMemberSqlite> list) {
        List<GroupMemberSqlite> result = new ArrayList<>(list);
        result.forEach(e -> e.setUserAvatarUrl(CommonUtil.getAvatarUrl(e.getUserId(), false)));
        return result;
    }
}
