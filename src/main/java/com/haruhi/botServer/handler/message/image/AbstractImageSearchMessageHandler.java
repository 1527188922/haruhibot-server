package com.haruhi.botServer.handler.message.image;

import com.haruhi.botServer.dto.qqclient.Message;
import com.haruhi.botServer.utils.ThreadPoolUtil;
import com.haruhi.botServer.ws.Bot;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public abstract class AbstractImageSearchMessageHandler implements com.haruhi.botServer.handler.message.IAllMessageHandler {

    private final ImageSearchProviderFactory imageSearchProviderFactory;
    private final ImageSearchTriggerResolver triggerResolver = new ImageSearchTriggerResolver();

    protected AbstractImageSearchMessageHandler(ImageSearchProviderFactory imageSearchProviderFactory) {
        this.imageSearchProviderFactory = imageSearchProviderFactory;
    }

    @Override
    public boolean onMessage(Bot bot, Message message) {
        if (!allow(bot, message)) {
            return false;
        }

        ImageSearchMatch match = triggerResolver.resolve(bot, message, regex());
        if (!match.isMatched()) {
            return false;
        }

        if (match.isWaitingImage()) {
            bot.sendMessage(message.getUserId(), message.getGroupId(), message.getMessageType(), waitingImageMessage(), true);
            return true;
        }

        startSearch(bot, message, match);
        return true;
    }

    protected boolean allow(Bot bot, Message message) {
        return true;
    }

    protected abstract String regex();

    protected ImageSearchProviderType providerType() {
        throw new UnsupportedOperationException("providerType must be supplied by single-provider handlers");
    }

    protected abstract String startSearchMessage();

    protected List<ImageSearchProvider> providers() {
        return List.of(imageSearchProviderFactory.getProvider(providerType()));
    }

    protected String waitingImageMessage() {
        return "图呢！";
    }

    protected String errorPrefix() {
        return "识图异常：";
    }

    private void startSearch(Bot bot, Message message, ImageSearchMatch match) {
        bot.sendMessage(message.getUserId(), message.getGroupId(), message.getMessageType(), startSearchMessage(), true);
        ThreadPoolUtil.getHandleCommandPool().execute(() -> {
            for (ImageSearchProvider provider : providers()) {
                try {
                    provider.search(bot, message, match.getReplyMessage(), match.getImageUrl());
                } catch (Exception e) {
                    log.error("{}{} provider:{}", errorPrefix(), match.getImageUrl(), provider.type(), e);
                    bot.sendMessage(message.getUserId(), message.getGroupId(), message.getMessageType(),
                            provider.type() + " " + errorPrefix() + e.getMessage(), true);
                }
            }
        });
        triggerResolver.clear(match.getCacheKey());
    }
}
