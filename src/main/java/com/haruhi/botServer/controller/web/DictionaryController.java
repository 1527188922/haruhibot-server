package com.haruhi.botServer.controller.web;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.haruhi.botServer.config.BotConfig;
import com.haruhi.botServer.controller.HttpResp;
import com.haruhi.botServer.entity.DictionarySqlite;
import com.haruhi.botServer.service.DictionarySqliteService;
import com.haruhi.botServer.vo.DictQueryReq;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(BotConfig.CONTEXT_PATH+"/dict")
public class DictionaryController {

    @Autowired
    private DictionarySqliteService dictionarySqliteService;


    @PostMapping("/search")
    public HttpResp<IPage<DictionarySqlite>> list(@RequestBody DictQueryReq request){
        IPage<DictionarySqlite> page = dictionarySqliteService.search(request, true);
        return HttpResp.success(page);
    }

    @PostMapping("/refresh")
    public HttpResp refreshCache(){
        long l = System.currentTimeMillis();
        dictionarySqliteService.refreshCache();
        return HttpResp.success("刷新完成，耗时："+(System.currentTimeMillis() - l),null);
    }
}
