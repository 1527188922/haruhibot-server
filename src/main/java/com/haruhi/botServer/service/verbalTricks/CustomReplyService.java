package com.haruhi.botServer.service.verbalTricks;

import com.baomidou.mybatisplus.extension.service.IService;
import com.haruhi.botServer.entity.CustomReply;

public interface CustomReplyService extends IService<CustomReply> {

    void loadToCache();
    
    void clearCache();
}
