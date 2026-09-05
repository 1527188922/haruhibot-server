package com.haruhi.botServer.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.haruhi.botServer.entity.SystemLogSqlite;
import com.haruhi.botServer.vo.SystemLogQueryReq;

public interface SystemLogSqliteService extends IService<SystemLogSqlite> {

    IPage<SystemLogSqlite> search(SystemLogQueryReq request);
}
