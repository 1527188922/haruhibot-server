package com.haruhi.botServer.event.notice;

import com.haruhi.botServer.dto.gocq.response.Message;
import org.springframework.web.socket.WebSocketSession;

/**
 * 实现这个接口的类,都能收到戳一戳消息
 */
public interface IPokeEvent extends INoticeEventType{

    void onPoke(WebSocketSession session,Message message);
}
