package com.haruhi.botServer.controller.web;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.haruhi.botServer.config.BotConfig;
import com.haruhi.botServer.entity.FriendSqlite;
import com.haruhi.botServer.service.FriendSqliteService;
import com.haruhi.botServer.vo.FriendInfoQueryReq;
import com.haruhi.botServer.vo.HttpResp;
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
