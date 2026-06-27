package com.haruhi.botServer.handler.notice;

import com.haruhi.botServer.dto.qqclient.Message;
import com.haruhi.botServer.ws.Bot;

/**
 * 实现这个接口的类,都能收到戳一戳消息
 */
public interface IPokeHandler extends INoticeHandler {

    void onPoke(Bot bot, Message message);
}
