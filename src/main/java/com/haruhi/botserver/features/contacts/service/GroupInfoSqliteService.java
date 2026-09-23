package com.haruhi.botserver.features.contacts.service;

import cn.hutool.core.lang.mutable.MutablePair;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.haruhi.botserver.features.contacts.persistence.entity.GroupInfoSqlite;
import com.haruhi.botserver.shared.model.CodeNameReq;
import com.haruhi.botserver.features.contacts.model.GroupCodeNameResp;
import com.haruhi.botserver.features.contacts.model.GroupInfoQueryReq;
import com.haruhi.botserver.bot.session.Bot;

import java.util.List;
import java.util.Map;

public interface GroupInfoSqliteService extends IService<GroupInfoSqlite> {

    MutablePair<List<GroupInfoSqlite>,List<GroupInfoSqlite>> loadGroupInfo(Bot bot);

    Map<Long,List<GroupInfoSqlite>> selectMapByGroupIds(List<Long> groupIds);

    IPage<GroupInfoSqlite> search(GroupInfoQueryReq request, boolean isPage);

    List<GroupInfoSqlite> selectBySelfId(Long selfId);

    List<GroupCodeNameResp> codeNameList(CodeNameReq request);

    boolean updateAndNull(GroupInfoSqlite entity);
}
