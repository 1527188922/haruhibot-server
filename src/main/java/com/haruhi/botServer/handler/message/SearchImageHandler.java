package com.haruhi.botServer.handler.message;

import com.haruhi.botServer.config.config.Configs;

import com.haruhi.botServer.config.config.ConfigKey;

import com.haruhi.botServer.constant.HandlerWeightEnum;
import com.haruhi.botServer.constant.RegexEnum;
import com.haruhi.botServer.dto.qqclient.Message;
import com.haruhi.botServer.handler.message.image.AbstractImageSearchMessageHandler;
import com.haruhi.botServer.handler.message.image.ImageSearchProviderFactory;
import com.haruhi.botServer.handler.message.image.ImageSearchProviderType;
import com.haruhi.botServer.ws.Bot;
import org.springframework.stereotype.Component;

@Component
public class SearchImageHandler extends AbstractImageSearchMessageHandler {


    public SearchImageHandler(ImageSearchProviderFactory imageSearchProviderFactory) {
        super(imageSearchProviderFactory);
    }

    @Override
    public int weight() {
        return HandlerWeightEnum.W_760.getWeight();
    }

    @Override
    public String funName() {
        return HandlerWeightEnum.W_760.getName();
    }

    @Override
    protected boolean allow(Bot bot, Message message) {
        boolean searchImageAllowGroup = Configs.getBool(ConfigKey.BOT_SWITCH_SEARCH_IMAGE_ALLOW_GROUP, false);
        return searchImageAllowGroup || !message.isGroupMsg();
    }

    @Override
    protected String regex() {
        return RegexEnum.SEARCH_IMAGE.getValue();
    }

    @Override
    protected ImageSearchProviderType providerType() {
        return ImageSearchProviderType.SAUCENAO;
    }

    @Override
    protected String startSearchMessage() {
        return "开始搜图...";
    }
}
