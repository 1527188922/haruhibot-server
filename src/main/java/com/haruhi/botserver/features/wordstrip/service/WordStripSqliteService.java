package com.haruhi.botserver.features.wordstrip.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.haruhi.botserver.features.wordstrip.persistence.entity.WordStripSqlite;
import com.haruhi.botserver.features.wordstrip.model.WordStripQueryReq;

public interface WordStripSqliteService extends IService<WordStripSqlite> {

    void loadWordStrip();

    void clearCache();

    IPage<WordStripSqlite> search(WordStripQueryReq request, boolean isPage);
}
