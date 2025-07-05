package com.haruhi.botServer.service.sqlite;

import com.baomidou.mybatisplus.extension.service.IService;
import com.haruhi.botServer.entity.sqlite.PokeReplySqlite;

public interface PokeReplySqliteService extends IService<PokeReplySqlite> {

    void loadPokeReply();

    void clearCache();

}
