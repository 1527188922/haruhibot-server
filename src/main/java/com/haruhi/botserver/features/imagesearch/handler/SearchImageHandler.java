package com.haruhi.botserver.features.imagesearch.handler;

import com.haruhi.botserver.configuration.service.Configs;

import com.haruhi.botserver.configuration.metadata.ConfigKey;

import com.haruhi.botserver.bot.handler.HandlerWeightEnum;
import com.haruhi.botserver.shared.constant.RegexEnum;
import com.haruhi.botserver.integration.onebot.model.Message;
import com.haruhi.botserver.bot.session.Bot;
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
