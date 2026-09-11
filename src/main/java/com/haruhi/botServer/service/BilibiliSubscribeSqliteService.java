package com.haruhi.botServer.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.haruhi.botServer.constant.BilibiliSubscribeTypeEnum;
import com.haruhi.botServer.entity.BilibiliSubscribeSqlite;

import java.util.List;

public interface BilibiliSubscribeSqliteService extends IService<BilibiliSubscribeSqlite> {

    /**
     * 查询已启用的订阅
     * @param subType 订阅类型 {@link BilibiliSubscribeTypeEnum}
     * @return
     */
    List<BilibiliSubscribeSqlite> listEnabled(String subType);
}
