package com.haruhi.botServer.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.haruhi.botServer.dto.qqclient.GroupInfo;
import com.haruhi.botServer.dto.qqclient.SyncResponse;
import com.haruhi.botServer.entity.GroupInfoSqlite;
import com.haruhi.botServer.mapper.GroupInfoSqliteMapper;
import com.haruhi.botServer.utils.DateTimeUtil;
import com.haruhi.botServer.ws.Bot;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class GroupInfoSqliteServiceImpl extends ServiceImpl<GroupInfoSqliteMapper, GroupInfoSqlite> implements GroupInfoSqliteService {


    /**
     * 获取机器人所有群 存入数据库
     * @param bot
     * @return 返回本次请求qq客户端获取的群
     */
    @Override
    public List<GroupInfoSqlite> loadGroupInfo(Bot bot) {
        SyncResponse<List<GroupInfo>> syncResponse = bot.getGroupList(true, 10 * 1000);
        if (!syncResponse.isSuccess()) {
            return Collections.emptyList();
        }
        List<GroupInfo> data = syncResponse.getData();
        if (CollectionUtils.isEmpty(data)) {
            return Collections.emptyList();
        }
        Long selfId = bot.getId();
        List<GroupInfoSqlite> groupInfoSqlites = data.stream().map(e -> {
            GroupInfoSqlite groupInfoSqlite = new GroupInfoSqlite();
            BeanUtils.copyProperties(e, groupInfoSqlite);
            groupInfoSqlite.setSelfId(selfId);
            Long groupCreateTime = e.getGroupCreateTime();
            if (Objects.nonNull(groupCreateTime)) {
                if(String.valueOf(groupCreateTime).length() == 10){
                    groupInfoSqlite.setGroupCreateTime(DateTimeUtil.dateTimeFormat(new Date(groupCreateTime * 1000), DateTimeUtil.PatternEnum.yyyyMMddHHmmss));
                }else{
                    groupInfoSqlite.setGroupCreateTime(DateTimeUtil.dateTimeFormat(groupCreateTime, DateTimeUtil.PatternEnum.yyyyMMddHHmmss));
                }
            }
            return groupInfoSqlite;
        }).collect(Collectors.toList());

        List<GroupInfoSqlite> dbList = this.list(new LambdaQueryWrapper<GroupInfoSqlite>()
                .eq(GroupInfoSqlite::getSelfId, selfId));

        List<GroupInfoSqlite> needAdd = new ArrayList<>();
        List<GroupInfoSqlite> needUpd = new ArrayList<>();

        groupInfoSqlites.forEach(groupInfoSqlite -> {
            GroupInfoSqlite dbData = findByGroupId(groupInfoSqlite.getGroupId(), dbList);
            if (dbData == null) {
                needAdd.add(groupInfoSqlite);
            }else{
                groupInfoSqlite.setId(dbData.getId());
                needUpd.add(groupInfoSqlite);
            }
        });

        if (CollectionUtils.isNotEmpty(needAdd)) {
            this.saveBatch(needAdd);
        }

        if (CollectionUtils.isNotEmpty(needUpd)) {
            // 先删除再新增
            this.removeByIds(needUpd.stream().map(GroupInfoSqlite::getId).collect(Collectors.toList()));
            needUpd.forEach(groupInfoSqlite -> groupInfoSqlite.setId(null));
            this.saveBatch(needUpd);
        }
        needAdd.addAll(needUpd);
//        List<Long> groupIds = needAdd.stream().map(GroupInfoSqlite::getGroupId).distinct().collect(Collectors.toList());
//        List<GroupInfoSqlite> groupInfoSqlites1 = dbList.stream().filter(e -> !groupIds.contains(e.getGroupId())).collect(Collectors.toList());
//        if (CollectionUtils.isNotEmpty(groupInfoSqlites1)) {
//            needAdd.addAll(groupInfoSqlites1);
//        }
        return needAdd;
    }

    private GroupInfoSqlite findByGroupId(Long groupId, List<GroupInfoSqlite> dbList) {
        return dbList.stream().filter(e -> groupId.equals(e.getGroupId())).findFirst().orElse(null);
    }

    @Override
    public Map<Long,List<GroupInfoSqlite>> selectMapByGroupIds(List<Long> groupIds) {
        if (CollectionUtils.isEmpty(groupIds)) {
            return Collections.emptyMap();
        }
        List<GroupInfoSqlite> list = this.list(new LambdaQueryWrapper<GroupInfoSqlite>()
                .in(GroupInfoSqlite::getGroupId, groupIds));
        return list.stream().collect(Collectors.groupingBy(GroupInfoSqlite::getGroupId,Collectors.toList()));
    }
}
