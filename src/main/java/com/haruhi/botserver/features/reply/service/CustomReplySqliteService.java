package com.haruhi.botserver.features.reply.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.haruhi.botserver.features.reply.persistence.entity.CustomReplySqlite;

public interface CustomReplySqliteService extends IService<CustomReplySqlite> {


    void loadToCache();

    void clearCache();
}
