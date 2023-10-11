package com.haruhi.botServer.service.chatRecord;

import com.baomidou.mybatisplus.extension.service.IService;
import com.haruhi.botServer.dto.gocq.response.Message;
import com.haruhi.botServer.entity.ChatRecord;
import com.haruhi.botServer.handlers.message.chatRecord.FindGroupChatHandler;
import com.haruhi.botServer.handlers.message.chatRecord.GroupWordCloudHandler;
import org.springframework.web.socket.WebSocketSession;

public interface ChatRecordService extends IService<ChatRecord> {

    /**
     * 根据时间发送聊天记录
     * @param message
     * @param param
     * @param session
     */
    void sendGroupChatList(WebSocketSession session, Message message, FindGroupChatHandler.Param param);

    void sendWordCloudImage(WebSocketSession session, GroupWordCloudHandler.RegexEnum regexEnum, Message message);
}
