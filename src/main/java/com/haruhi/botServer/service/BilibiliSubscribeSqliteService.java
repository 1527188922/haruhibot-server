package com.haruhi.botServer.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.haruhi.botServer.constant.BilibiliSubscribeTypeEnum;
import com.haruhi.botServer.entity.BilibiliSubscribeSqlite;
import com.haruhi.botServer.vo.BilibiliSubscribeQueryReq;
import com.haruhi.botServer.vo.BilibiliSubscribeResp;
import com.haruhi.botServer.vo.PushTargetInfo;
import com.haruhi.botServer.vo.PushTargetQueryReq;

import java.util.List;

public interface BilibiliSubscribeSqliteService extends IService<BilibiliSubscribeSqlite> {

    /**
     * 查询已启用的订阅
     * @param subType 订阅类型 {@link BilibiliSubscribeTypeEnum}
     * @return
     */
    List<BilibiliSubscribeSqlite> listEnabled(String subType);

    /**
     * web管理界面分页查询
     */
    IPage<BilibiliSubscribeResp> search(BilibiliSubscribeQueryReq request, boolean isPage);

    /**
     * 新增订阅
     */
    boolean saveSubscribe(BilibiliSubscribeSqlite entity);

    /**
     * 修改订阅(不包含推送目标)
     */
    boolean updateSubscribe(BilibiliSubscribeSqlite entity);

    /**
     * 修改推送的群与好友
     */
    boolean updateTargets(Long id, List<Long> groupIds, List<Long> friendIds);

    /**
     * 更新主播昵称与头像，定时任务请求到直播状态数据时调用
     */
    boolean refreshLiveInfo(Long uid, String uname, String face);

    /**
     * 查询可选的推送目标(群/好友)，用于web管理界面选择
     */
    List<PushTargetInfo> listTargetCandidates(PushTargetQueryReq request);

    /**
     * 是否已存在相同的订阅
     * @param excludeId 需要排除的订阅id(修改时排除自身)，可为null
     */
    boolean existsSubscribe(Long uid, Long selfId, String subType, Long excludeId);
}
