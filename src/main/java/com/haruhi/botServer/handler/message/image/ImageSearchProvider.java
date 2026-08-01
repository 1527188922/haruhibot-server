package com.haruhi.botServer.handler.message.image;

import com.haruhi.botServer.dto.qqclient.Message;
import com.haruhi.botServer.ws.Bot;

public interface ImageSearchProvider {

    ImageSearchProviderType type();

    void search(Bot bot, Message message, Message replyMessage, String imageUrl);
}
