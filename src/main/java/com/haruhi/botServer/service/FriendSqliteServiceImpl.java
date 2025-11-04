package com.haruhi.botServer.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.haruhi.botServer.dto.qqclient.FriendInfo;
import com.haruhi.botServer.dto.qqclient.SyncResponse;
import com.haruhi.botServer.entity.FriendSqlite;
import com.haruhi.botServer.mapper.FriendSqliteMapper;
import com.haruhi.botServer.ws.Bot;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
public class FriendSqliteServiceImpl extends ServiceImpl<FriendSqliteMapper, FriendSqlite> implements FriendSqliteService {

    @Transactional
    @Override
    public List<FriendSqlite> loadFriendInfo(Bot bot) {
        Long selfId = bot.getId();
        if(Objects.isNull(selfId)){
            log.info("加载好友失败selfId为空");
            return Collections.emptyList();
        }
        SyncResponse<List<FriendInfo>> response = bot.getFriendList(false, 5 * 1000);
        if (!response.isSuccess()) {
            log.info("加载好友失败 {}",response);
            return Collections.emptyList();
        }
        List<FriendInfo> data = response.getData();
        if (CollectionUtils.isEmpty(data)) {
            return Collections.emptyList();
        }

        List<FriendSqlite> newFriends = data.stream().map(friendInfo -> {
            FriendSqlite friendSqlite = new FriendSqlite();
            BeanUtils.copyProperties(friendInfo, friendSqlite);
            friendSqlite.setSelfId(selfId);
            return friendSqlite;
        }).collect(Collectors.toList());

        List<FriendSqlite> dbList = this.list(new LambdaQueryWrapper<FriendSqlite>()
                .eq(FriendSqlite::getSelfId, selfId));

        ArrayList<FriendSqlite> needAdd = new ArrayList<>();
        ArrayList<FriendSqlite> needUpd = new ArrayList<>();

        newFriends.forEach(newFriend -> {
            FriendSqlite dbFriend = findByUserId(newFriend.getUserId(), dbList);
            if (dbFriend == null) {
                needAdd.add(newFriend);
            }else{
                newFriend.setId(dbFriend.getId());
                needUpd.add(newFriend);
            }
        });

        if (CollectionUtils.isNotEmpty(needAdd)) {
            this.saveBatch(needAdd);
        }
        if (CollectionUtils.isNotEmpty(needUpd)) {
            this.removeByIds(needUpd.stream().map(FriendSqlite::getId).collect(Collectors.toList()));
            needUpd.forEach(newFriend -> newFriend.setId(null));
            this.saveBatch(needUpd);
        }
        return newFriends;
    }

    private FriendSqlite findByUserId(Long userId, List<FriendSqlite> dbList) {
        return dbList.stream().filter(e -> userId.equals(e.getUserId())).findFirst().orElse(null);
    }
}
