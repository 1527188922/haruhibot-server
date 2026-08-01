package com.haruhi.botServer.handler.message.image;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.haruhi.botServer.dto.qqclient.ForwardMsgItem;
import com.haruhi.botServer.dto.qqclient.Message;
import com.haruhi.botServer.dto.qqclient.MessageHolder;
import com.haruhi.botServer.dto.trace.SearchResp;
import com.haruhi.botServer.utils.CommonUtil;
import com.haruhi.botServer.ws.Bot;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class TraceMoeAnimeSearchProvider implements ImageSearchProvider {

    private static final String TRACE_API = "https://api.trace.moe/search?cutBorders";
    private static final int SEARCH_TIMEOUT = 10 * 1000;

    @Override
    public ImageSearchProviderType type() {
        return ImageSearchProviderType.TRACE_MOE;
    }

    @Override
    public void search(Bot bot, Message message, Message replyMessage, String imageUrl) {
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("url", imageUrl);
        String urlWithParams = HttpUtil.urlWithForm(TRACE_API, hashMap, StandardCharsets.UTF_8, false);
        HttpRequest httpRequest = HttpUtil.createGet(urlWithParams).timeout(SEARCH_TIMEOUT);
        try (HttpResponse response = httpRequest.execute()) {
            if (!response.isOk()) {
                bot.sendMessage(message.getUserId(), message.getGroupId(), message.getMessageType(), MessageHolder.instanceText("搜番异常：" + response.getStatus()));
                return;
            }
            SearchResp<Object> searchResp = JSONObject.parseObject(response.body(), new TypeReference<SearchResp<Object>>() { });
            if (StringUtils.isNotBlank(searchResp.getError())) {
                bot.sendMessage(message.getUserId(), message.getGroupId(), message.getMessageType(), MessageHolder.instanceText(searchResp.getError()));
                return;
            }
            List<SearchResp.Result<Object>> result = searchResp.getResult();
            if (CollectionUtils.isEmpty(result)) {
                bot.sendMessage(message.getUserId(), message.getGroupId(), message.getMessageType(), MessageHolder.instanceText("未搜索到结果"));
                return;
            }
            bot.sendForwardMessage(message.getUserId(), message.getGroupId(), message.getMessageType(), resultToForwardMsgItems(bot, message, result));
        }
    }

    private List<ForwardMsgItem> resultToForwardMsgItems(Bot bot, Message message, List<SearchResp.Result<Object>> results) {
        List<ForwardMsgItem> forwardMsgItems = new ArrayList<>();
        for (SearchResp.Result<Object> result : results) {
            List<MessageHolder> messageHolders = new ArrayList<>();
            if (StringUtils.isNotBlank(result.getImage())) {
                messageHolders.add(MessageHolder.instanceImage(result.getImage()));
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(result.getFilename());
            stringBuilder.append("\n");
            stringBuilder.append("相似度：").append(numberFormat(result.getSimilarity()));

            if (result.getEpisode() != null) {
                stringBuilder.append("\n");
                stringBuilder.append("第").append(result.getEpisode()).append("集")
                        .append(CommonUtil.formatDuration((long) result.getAt().floatValue(), TimeUnit.SECONDS));
            } else if (result.getAt() != null) {
                stringBuilder.append("\n");
                stringBuilder.append("第").append(CommonUtil.formatDuration((long) result.getAt().floatValue(), TimeUnit.SECONDS));
            }
            if (StringUtils.isNotBlank(result.getVideo())) {
                stringBuilder.append("\n");
                stringBuilder.append("预览视频：").append(result.getVideo());
            }
            messageHolders.addAll(MessageHolder.instanceText(stringBuilder.toString()));
            forwardMsgItems.add(ForwardMsgItem.instance(message.getSelfId(), bot.getBotName(), messageHolders));
        }
        return forwardMsgItems;
    }

    private String numberFormat(Float num) {
        NumberFormat nf = NumberFormat.getPercentInstance();
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        return nf.format(num);
    }
}
