package com.haruhi.botServer.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.haruhi.botServer.entity.GroupMemberSqlite;
import com.haruhi.botServer.vo.GroupMemberQueryReq;
import com.haruhi.botServer.vo.GroupMemberRefreshResp;
import com.haruhi.botServer.ws.Bot;

public interface GroupMemberSqliteService extends IService<GroupMemberSqlite> {

    /**
     * 从qq客户端拉取群成员并入库
     * 不在最新数据中的群员标记为离群（只标记不删除）
     * @param bot 机器人
     * @param groupId 群号
     * @param timeout 获取群成员超时时间
     * @return 本次刷新结果
     */
    GroupMemberRefreshResp refreshGroupMember(Bot bot, Long groupId, long timeout);

    IPage<GroupMemberSqlite> search(GroupMemberQueryReq request);
}
