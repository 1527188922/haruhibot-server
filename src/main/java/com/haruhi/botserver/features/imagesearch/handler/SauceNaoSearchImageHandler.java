package com.haruhi.botserver.features.imagesearch.handler;

import com.haruhi.botserver.bot.handler.HandlerWeightEnum;
import com.haruhi.botserver.shared.constant.RegexEnum;
import org.springframework.stereotype.Component;

@Component
public class SauceNaoSearchImageHandler extends AbstractImageSearchMessageHandler {


    public SauceNaoSearchImageHandler(ImageSearchProviderFactory imageSearchProviderFactory) {
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
