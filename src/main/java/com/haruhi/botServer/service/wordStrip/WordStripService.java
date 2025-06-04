package com.haruhi.botServer.service.wordStrip;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.haruhi.botServer.entity.WordStrip;
import com.haruhi.botServer.vo.WordStripQueryReq;

public interface WordStripService extends IService<WordStrip> {

    void loadWordStrip();
    
    void clearCache();

    IPage<WordStrip> search(WordStripQueryReq request, boolean isPage);
}
