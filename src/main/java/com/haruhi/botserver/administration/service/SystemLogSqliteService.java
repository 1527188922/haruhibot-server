package com.haruhi.botserver.administration.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.haruhi.botserver.administration.persistence.entity.SystemLogSqlite;
import com.haruhi.botserver.administration.model.SystemLogQueryReq;

public interface SystemLogSqliteService extends IService<SystemLogSqlite> {

    IPage<SystemLogSqlite> search(SystemLogQueryReq request);
}
