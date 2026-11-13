package com.haruhi.botserver.features.imagesearch.handler;

import com.haruhi.botserver.bot.session.Bot;
import com.haruhi.botserver.features.imagesearch.client.PicImageSearchFactory;
import com.haruhi.botserver.features.imagesearch.client.SearchInput;
import com.haruhi.botserver.features.imagesearch.client.SearchItem;
import com.haruhi.botserver.features.imagesearch.client.SearchResponse;
import com.haruhi.botserver.integration.onebot.model.ForwardMsgItem;
import com.haruhi.botserver.integration.onebot.model.Message;
import com.haruhi.botserver.integration.onebot.model.MessageHolder;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Yandex 以图搜图返回包含该图片的网页列表
 * <p>
 * 实测：国内网络直连会返回 “The service is under construction.” 拦截页，无法获取结果，需要科学上网
 */
@Component
public class YandexImageSearchProvider implements ImageSearchProvider {

    private static final int MAX_RESULTS = 10;

    private final PicImageSearchFactory picImageSearchFactory;

    public YandexImageSearchProvider(PicImageSearchFactory picImageSearchFactory) {
        this.picImageSearchFactory = picImageSearchFactory;
    }

    @Override
    public ImageSearchProviderType type() {
        return ImageSearchProviderType.YANDEX;
    }

    @Override
    public void search(Bot bot, Message message, Message replyMessage, String imageUrl) {
        SearchResponse searchResponse = picImageSearchFactory.yandex().search(SearchInput.byUrl(imageUrl));
        List<SearchItem> results = searchResponse.getRaw();
        if (CollectionUtils.isEmpty(results)) {
            bot.sendMessage(message.getUserId(), message.getGroupId(), message.getMessageType(),
                    MessageHolder.instanceText("Yandex：未搜索到结果"));
            return;
        }
        List<ForwardMsgItem> forwardMsgs = new ArrayList<>();
        for (int i = 0; i < results.size() && i < MAX_RESULTS; i++) {
            forwardMsgs.add(ForwardMsgItem.instance(message.getSelfId(), bot.getBotName(),
                    resultToMessageHolders(results.get(i))));
        }
        bot.sendForwardMessage(message.getUserId(), message.getGroupId(), message.getMessageType(), forwardMsgs);
    }

    private List<MessageHolder> resultToMessageHolders(SearchItem result) {
        List<MessageHolder> messageHolders = new ArrayList<>();
        if (StringUtils.isNotBlank(result.getThumbnail())) {
            messageHolders.add(MessageHolder.instanceImage(result.getThumbnail()));
        }
        messageHolders.addAll(MessageHolder.instanceText(itemMsg(result)));
        return messageHolders;
    }

    private String itemMsg(SearchItem result) {
        String title = result.getTitle();
        String url = result.getUrl();
        if (StringUtils.isNotBlank(url)) {
            return "标题：" + title + "\n链接：" + url;
        }
        return "标题：" + title;
    }
}
