package com.haruhi.botserver.features.contacts.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.haruhi.botserver.bootstrap.BotConfig;
import com.haruhi.botserver.features.contacts.persistence.entity.FriendSqlite;
import com.haruhi.botserver.features.contacts.service.FriendSqliteService;
import com.haruhi.botserver.features.contacts.model.FriendInfoQueryReq;
import com.haruhi.botserver.shared.model.HttpResp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(BotConfig.CONTEXT_PATH+"/friend")
public class FriendController {

    @Autowired
    private FriendSqliteService friendSqliteService;


    @PostMapping("/search")
    public HttpResp<IPage<FriendSqlite>> search(@RequestBody FriendInfoQueryReq request){
        IPage<FriendSqlite> page = friendSqliteService.search(request, true);
        return HttpResp.success(page);
    }

}
