package com.haruhi.botServer.handler.message;

import com.haruhi.botServer.constant.DictionaryEnum;
import com.haruhi.botServer.constant.HandlerWeightEnum;
import com.haruhi.botServer.constant.RegexEnum;
import com.haruhi.botServer.dto.qqclient.Message;
import com.haruhi.botServer.handler.message.image.AbstractImageSearchMessageHandler;
import com.haruhi.botServer.handler.message.image.ImageSearchProviderFactory;
import com.haruhi.botServer.handler.message.image.ImageSearchProviderType;
import com.haruhi.botServer.service.DictionarySqliteService;
import com.haruhi.botServer.ws.Bot;
import org.springframework.stereotype.Component;

@Component
public class SearchImageHandler extends AbstractImageSearchMessageHandler {

    private final DictionarySqliteService dictionarySqliteService;

    public SearchImageHandler(ImageSearchProviderFactory imageSearchProviderFactory,
                              DictionarySqliteService dictionarySqliteService) {
        super(imageSearchProviderFactory);
        this.dictionarySqliteService = dictionarySqliteService;
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
        boolean searchImageAllowGroup = dictionarySqliteService.getBoolean(DictionaryEnum.SWITCH_SEARCH_IMAGE_ALLOW_GROUP.getKey(), false);
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
