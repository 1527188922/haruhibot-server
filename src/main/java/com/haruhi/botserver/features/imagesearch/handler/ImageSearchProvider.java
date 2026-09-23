package com.haruhi.botserver.features.imagesearch.handler;

import com.haruhi.botserver.integration.onebot.model.Message;
import com.haruhi.botserver.bot.session.Bot;

public interface ImageSearchProvider {

    ImageSearchProviderType type();

    void search(Bot bot, Message message, Message replyMessage, String imageUrl);
}
