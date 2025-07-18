package com.haruhi.botServer.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.haruhi.botServer.dto.qqclient.Message;
import com.haruhi.botServer.entity.PixivSqlite;
import com.haruhi.botServer.ws.Bot;

import java.util.List;


public interface PixivSqliteService extends IService<PixivSqlite> {

    void roundSend(Bot bot, int num, Boolean isR18, List<String> tags, Message message, String tag);
}
