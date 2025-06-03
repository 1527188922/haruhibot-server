package com.haruhi.botServer.service.chatRecord;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.haruhi.botServer.dto.gocq.response.Message;
import com.haruhi.botServer.entity.ChatRecord;
import com.haruhi.botServer.handlers.message.chatRecord.FindGroupChatHandler;
import com.haruhi.botServer.handlers.message.chatRecord.GroupWordCloudHandler;
import com.haruhi.botServer.vo.ChatRecordQueryReq;
import com.haruhi.botServer.ws.Bot;

public interface ChatRecordService extends IService<ChatRecord> {

    /**
     * 根据时间发送聊天记录
     * @param message
     * @param param
     * @param bot
     */
    void sendGroupChatList(Bot bot, Message message, FindGroupChatHandler.Param param);

    void sendWordCloudImage(Bot bot, GroupWordCloudHandler.RegexEnum regexEnum, Message message);

    IPage<ChatRecord> search(ChatRecordQueryReq request, boolean page);
}
