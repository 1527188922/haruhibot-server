package com.haruhi.botServer.handler.message;

import com.haruhi.botServer.constant.HandlerWeightEnum;
import com.haruhi.botServer.constant.RegexEnum;
import com.haruhi.botServer.handler.message.image.AbstractImageSearchMessageHandler;
import com.haruhi.botServer.handler.message.image.ImageSearchProvider;
import com.haruhi.botServer.handler.message.image.ImageSearchProviderFactory;
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
