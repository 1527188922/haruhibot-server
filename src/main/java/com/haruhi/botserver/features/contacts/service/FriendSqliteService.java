package com.haruhi.botserver.features.contacts.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.haruhi.botserver.features.contacts.persistence.entity.FriendSqlite;
import com.haruhi.botserver.features.contacts.model.FriendInfoQueryReq;
import com.haruhi.botserver.bot.session.Bot;

import java.util.List;

public interface FriendSqliteService extends IService<FriendSqlite> {

    List<FriendSqlite> loadFriendInfo(Bot bot);

    boolean updateAndNull(FriendSqlite entity);

    IPage<FriendSqlite> search(FriendInfoQueryReq request, boolean b);
}
