package com.haruhi.botserver.features.imagesearch.handler;

import com.haruhi.botserver.bot.handler.HandlerWeightEnum;
import com.haruhi.botserver.bot.session.Bot;
import com.haruhi.botserver.configuration.metadata.ConfigKey;
import com.haruhi.botserver.configuration.service.Configs;
import com.haruhi.botserver.integration.onebot.model.Message;
import com.haruhi.botserver.shared.constant.RegexEnum;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 聚合识图handler
 */
@Component
public class AllImageSearchHandler extends AbstractImageSearchMessageHandler {

    private final ImageSearchProviderFactory imageSearchProviderFactory;

    public AllImageSearchHandler(ImageSearchProviderFactory imageSearchProviderFactory) {
        super(imageSearchProviderFactory);
        this.imageSearchProviderFactory = imageSearchProviderFactory;
    }

    @Override
    public int weight() {
        return HandlerWeightEnum.W_770.getWeight();
    }

    @Override
    public String funName() {
        return HandlerWeightEnum.W_770.getName();
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
    protected List<ImageSearchProvider> providers() {
        return imageSearchProviderFactory.getAllProviders();
    }

    @Override
    protected String startSearchMessage() {
        return "开始聚合识图...";
    }
}
