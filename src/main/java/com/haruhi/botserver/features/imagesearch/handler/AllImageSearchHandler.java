package com.haruhi.botserver.features.imagesearch.handler;

import com.haruhi.botserver.bot.handler.HandlerWeightEnum;
import com.haruhi.botserver.shared.constant.RegexEnum;
import org.springframework.stereotype.Component;

import java.util.List;

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
