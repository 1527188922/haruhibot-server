package com.haruhi.botServer.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.haruhi.botServer.entity.FriendSqlite;
import com.haruhi.botServer.ws.Bot;

import java.util.List;

public interface FriendSqliteService extends IService<FriendSqlite> {

    List<FriendSqlite> loadFriendInfo(Bot bot);
}
