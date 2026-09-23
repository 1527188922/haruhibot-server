package com.haruhi.botserver.features.imagesearch.handler;

import com.haruhi.botserver.bot.handler.HandlerWeightEnum;
import com.haruhi.botserver.shared.constant.RegexEnum;
import org.springframework.stereotype.Component;

/**
 * 根据图片搜索番剧
 */
@Component
public class TraceSearchAnimeHandler extends AbstractImageSearchMessageHandler {

    public TraceSearchAnimeHandler(ImageSearchProviderFactory imageSearchProviderFactory) {
        super(imageSearchProviderFactory);
    }

    @Override
    public int weight() {
        return HandlerWeightEnum.W_750.getWeight();
    }

    @Override
    public String funName() {
        return HandlerWeightEnum.W_750.getName();
    }

    @Override
    protected String regex() {
        return RegexEnum.SEARCH_ANIME.getValue();
    }

    @Override
    protected ImageSearchProviderType providerType() {
        return ImageSearchProviderType.TRACE_MOE;
    }

    @Override
    protected String startSearchMessage() {
        return "开始搜番...";
    }

    @Override
    protected String errorPrefix() {
        return "搜番异常：";
    }
}
