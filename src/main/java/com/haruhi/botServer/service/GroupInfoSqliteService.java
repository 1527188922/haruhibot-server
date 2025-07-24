package com.haruhi.botServer.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.haruhi.botServer.entity.GroupInfoSqlite;
import com.haruhi.botServer.ws.Bot;

import java.util.List;
import java.util.Map;

public interface GroupInfoSqliteService extends IService<GroupInfoSqlite> {

    List<GroupInfoSqlite> loadGroupInfo(Bot bot);

    Map<Long,List<GroupInfoSqlite>> selectMapByGroupIds(List<Long> groupIds);

    IPage<GroupInfoSqlite> search(GroupInfoSqlite request, boolean isPage);
}
