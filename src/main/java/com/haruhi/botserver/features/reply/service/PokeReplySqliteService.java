package com.haruhi.botserver.features.reply.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.haruhi.botserver.features.reply.persistence.entity.PokeReplySqlite;

public interface PokeReplySqliteService extends IService<PokeReplySqlite> {

    void loadPokeReply();

    void clearCache();

}
