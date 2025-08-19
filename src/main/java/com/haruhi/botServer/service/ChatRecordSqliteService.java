package com.haruhi.botServer.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.haruhi.botServer.dto.BaseResp;
import com.haruhi.botServer.dto.qqclient.Message;
import com.haruhi.botServer.entity.ChatRecordSqlite;
import com.haruhi.botServer.handlers.message.chatRecord.FindGroupChatHandler;
import com.haruhi.botServer.handlers.message.chatRecord.GroupWordCloudHandler;
import com.haruhi.botServer.vo.ChatRecordQueryReq;
import com.haruhi.botServer.ws.Bot;

import java.io.File;
import java.util.List;

public interface ChatRecordSqliteService extends IService<ChatRecordSqlite> {


    /**
     * 根据时间发送聊天记录
     * @param message
     * @param param
     * @param bot
     */
    void sendGroupChatList(Bot bot, Message message, FindGroupChatHandler.Param param);

    void sendWordCloudImage(Bot bot, GroupWordCloudHandler.RegexEnum regexEnum, Message message);

    IPage<ChatRecordSqlite> search(ChatRecordQueryReq request, boolean isPage);

    BaseResp<File> exportGroupChatRecord(Long groupId, List<String> qqs);
}
