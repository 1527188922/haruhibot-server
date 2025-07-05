package com.haruhi.botServer.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.haruhi.botServer.entity.sqlite.WordStripSqlite;
import com.haruhi.botServer.vo.WordStripQueryReq;

public interface WordStripSqliteService extends IService<WordStripSqlite> {

    void loadWordStrip();

    void clearCache();

    IPage<WordStripSqlite> search(WordStripQueryReq request, boolean isPage);
}
