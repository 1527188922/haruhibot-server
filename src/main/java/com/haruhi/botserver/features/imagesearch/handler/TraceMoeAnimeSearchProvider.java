package com.haruhi.botserver.features.imagesearch.handler;

import com.haruhi.botserver.integration.onebot.model.ForwardMsgItem;
import com.haruhi.botserver.integration.onebot.model.Message;
import com.haruhi.botserver.integration.onebot.model.MessageHolder;
import com.haruhi.botserver.features.imagesearch.client.PicImageSearchFactory;
import com.haruhi.botserver.features.imagesearch.client.SearchInput;
import com.haruhi.botserver.features.imagesearch.client.SearchItem;
import com.haruhi.botserver.features.imagesearch.client.SearchResponse;
import com.haruhi.botserver.shared.util.CommonUtil;
import com.haruhi.botserver.bot.session.Bot;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class TraceMoeAnimeSearchProvider implements ImageSearchProvider {

    private final PicImageSearchFactory picImageSearchFactory;

    public TraceMoeAnimeSearchProvider(PicImageSearchFactory picImageSearchFactory) {
        this.picImageSearchFactory = picImageSearchFactory;
    }

    @Override
    public ImageSearchProviderType type() {
        return ImageSearchProviderType.TRACE_MOE;
    }

    @Override
    public void search(Bot bot, Message message, Message replyMessage, String imageUrl) {
        SearchResponse searchResponse = picImageSearchFactory.traceMoe().search(SearchInput.byUrl(imageUrl));
        Object error = searchResponse.getExtra().get("error");
        if (error != null && StringUtils.isNotBlank(String.valueOf(error))) {
            bot.sendMessage(message.getUserId(), message.getGroupId(), message.getMessageType(), MessageHolder.instanceText(String.valueOf(error)));
            return;
        }
        List<SearchItem> result = searchResponse.getRaw();
        if (CollectionUtils.isEmpty(result)) {
            bot.sendMessage(message.getUserId(), message.getGroupId(), message.getMessageType(), MessageHolder.instanceText("未搜索到结果"));
            return;
        }
        bot.sendForwardMessage(message.getUserId(), message.getGroupId(), message.getMessageType(), resultToForwardMsgItems(bot, message, result));
    }

    private List<ForwardMsgItem> resultToForwardMsgItems(Bot bot, Message message, List<SearchItem> results) {
        List<ForwardMsgItem> forwardMsgItems = new ArrayList<>();
        for (SearchItem result : results) {
            List<MessageHolder> messageHolders = new ArrayList<>();
            if (StringUtils.isNotBlank(result.getImageUrl())) {
                messageHolders.add(MessageHolder.instanceImage(result.getImageUrl()));
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(value(result.getExtra().get("filename")));
            stringBuilder.append("\n");
            stringBuilder.append("相似度：").append(numberFormat(result.getSimilarity()));

            Object episode = result.getExtra().get("episode");
            Object from = result.getExtra().get("from");
            if (episode != null) {
                stringBuilder.append("\n");
                stringBuilder.append("第").append(episode).append("集 ")
                        .append(formatTraceTime(from));
            } else if (from != null) {
                stringBuilder.append("\n");
                stringBuilder.append("第").append(formatTraceTime(from));
            }
            String video = value(result.getExtra().get("video"));
            if (StringUtils.isNotBlank(video)) {
                stringBuilder.append("\n");
                stringBuilder.append("预览视频：").append(video);
            }
            messageHolders.addAll(MessageHolder.instanceText(stringBuilder.toString()));
            forwardMsgItems.add(ForwardMsgItem.instance(message.getSelfId(), bot.getBotName(), messageHolders));
        }
        return forwardMsgItems;
    }

    private String numberFormat(double num) {
        NumberFormat nf = NumberFormat.getPercentInstance();
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        return nf.format(num / 100D);
    }

    private String formatTraceTime(Object value) {
        if (value instanceof Number number) {
            return CommonUtil.formatDuration(number.longValue(), TimeUnit.SECONDS);
        }
        return "";
    }

    private String value(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
