package com.haruhi.botServer.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.haruhi.botServer.dto.gocq.response.Message;
import com.haruhi.botServer.entity.sqlite.PixivSqlite;
import com.haruhi.botServer.ws.Bot;

import java.util.List;


public interface PixivSqliteService extends IService<PixivSqlite> {

    void roundSend(Bot bot, int num, Boolean isR18, List<String> tags, Message message, String tag);
}
