package com.haruhi.botServer.service.groupChatHistory;

import com.baomidou.mybatisplus.extension.service.IService;
import com.haruhi.botServer.dto.gocq.response.Message;
import com.haruhi.botServer.entity.GroupChatHistory;
import com.haruhi.botServer.handlers.message.chatHistory.FindChatMessageHandler;
import com.haruhi.botServer.handlers.message.chatHistory.GroupWordCloudHandler;
import org.springframework.web.socket.WebSocketSession;

public interface GroupChatHistoryService extends IService<GroupChatHistory> {

    /**
     * 根据时间发送聊天记录
     * @param message
     * @param param
     * @param session
     */
    void sendChatList(WebSocketSession session,Message message, FindChatMessageHandler.Param param);

    void sendWordCloudImage(WebSocketSession session, GroupWordCloudHandler.RegexEnum regexEnum, Message message);
}
