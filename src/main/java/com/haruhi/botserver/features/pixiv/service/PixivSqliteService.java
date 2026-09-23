package com.haruhi.botserver.features.pixiv.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.haruhi.botserver.integration.onebot.model.Message;
import com.haruhi.botserver.features.pixiv.persistence.entity.PixivSqlite;
import com.haruhi.botserver.bot.session.Bot;

import java.util.List;


public interface PixivSqliteService extends IService<PixivSqlite> {

    void roundSend(Bot bot, int num, Boolean isR18, List<String> tags, Message message, String tag);
}
