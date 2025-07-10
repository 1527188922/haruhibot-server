package com.haruhi.botServer.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.haruhi.botServer.entity.CustomReplySqlite;

public interface CustomReplySqliteService extends IService<CustomReplySqlite> {


    void loadToCache();

    void clearCache();
}
