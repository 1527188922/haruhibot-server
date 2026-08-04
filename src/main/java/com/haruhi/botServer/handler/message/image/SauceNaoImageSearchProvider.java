package com.haruhi.botServer.handler.message.image;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.haruhi.botServer.constant.DictionaryEnum;
import com.haruhi.botServer.dto.qqclient.ForwardMsgItem;
import com.haruhi.botServer.dto.qqclient.Message;
import com.haruhi.botServer.dto.qqclient.MessageHolder;
import com.haruhi.botServer.picimagesearch.PicImageSearchFactory;
import com.haruhi.botServer.picimagesearch.SearchInput;
import com.haruhi.botServer.picimagesearch.SearchItem;
import com.haruhi.botServer.picimagesearch.SearchResponse;
import com.haruhi.botServer.service.DictionarySqliteService;
import com.haruhi.botServer.ws.Bot;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Component
public class SauceNaoImageSearchProvider implements ImageSearchProvider {

    private final DictionarySqliteService dictionarySqliteService;
    private final PicImageSearchFactory picImageSearchFactory;

    public SauceNaoImageSearchProvider(DictionarySqliteService dictionarySqliteService,
                                       PicImageSearchFactory picImageSearchFactory) {
        this.dictionarySqliteService = dictionarySqliteService;
        this.picImageSearchFactory = picImageSearchFactory;
    }

    @Override
    public ImageSearchProviderType type() {
        return ImageSearchProviderType.SAUCENAO;
    }

    @Override
    public void search(Bot bot, Message message, Message replyMessage, String imageUrl) {
        String apiKey = dictionarySqliteService.getInCache(DictionaryEnum.SAUCENAO_SEARCH_IMAGE__KEY.getKey(), null);
        log.info("开始请求搜图接口 图片:{}", imageUrl);
        try {
            SearchResponse searchResponse = picImageSearchFactory.sauceNao()
                    .apiKey(apiKey)
                    .search(SearchInput.byUrl(imageUrl));
            List<SearchItem> resultList = new ArrayList<>(searchResponse.getRaw());
            if (CollectionUtils.isEmpty(resultList)) {
                bot.sendMessage(message.getUserId(), message.getGroupId(), message.getMessageType(), "搜索结果为空", true);
                return;
            }
            resultList.sort(Comparator.comparingDouble(SearchItem::getSimilarity).reversed());
            sendResult(bot, resultList, message);
        } catch (Exception e) {
            bot.sendMessage(message.getUserId(), message.getGroupId(), message.getMessageType(), "搜图异常：" + e.getMessage(), true);
            log.error("搜图异常", e);
        }
    }

    private void sendResult(Bot bot, List<SearchItem> resultList, Message message) {
        List<ForwardMsgItem> forwardMsgs = new ArrayList<>();
        for (SearchItem result : resultList) {
            forwardMsgs.add(ForwardMsgItem.instance(message.getSelfId(), bot.getBotName(), MessageHolder.instanceText(getItemMsg(result))));
        }
        bot.sendForwardMessage(message.getUserId(), message.getGroupId(), message.getMessageType(), forwardMsgs);
    }

    private String getItemMsg(SearchItem result) {
        StringBuilder strBui = new StringBuilder();
        if (result.getSimilarity() > 0D) {
            strBui.append(MessageFormat.format("相似度：{0}\n", result.getSimilarity() + "%"));
        }
        appendLine(strBui, "标题", result.getTitle());
        appendLine(strBui, "来源", result.getSource());
        appendLine(strBui, "数据来源", stringExtra(result, "index_name"));

        JSONObject data = originData(result);
        appendLine(strBui, "日语名", data == null ? null : data.getString("jp_name"));
        appendLine(strBui, "出处", data == null ? null : data.getString("material"));
        String pixivId = data == null ? null : data.getString("pixiv_id");
        appendLine(strBui, "pid", pixivId);
        appendLine(strBui, "作者", firstNonBlank(data == null ? null : data.getString("member_name"), result.getAuthor()));
        appendCreator(strBui, data);
        appendLine(strBui, "twitter作者id", data == null ? null : data.getString("twitter_user_id"));
        appendExtUrls(strBui, result, data);
        appendLine(strBui, "缩略图", result.getThumbnail());
        if (StringUtils.isNotBlank(pixivId)) {
            strBui.append(MessageFormat.format("原图链接：https://pixiv.re/{0}.jpg", pixivId));
        }
        return strBui.toString();
    }

    private JSONObject originData(SearchItem result) {
        if (result.getOrigin() instanceof JSONObject origin) {
            return origin.getJSONObject("data");
        }
        return null;
    }

    private void appendCreator(StringBuilder strBui, JSONObject data) {
        if (data == null) {
            return;
        }
        Object creator = data.get("creator");
        if (creator == null) {
            return;
        }
        if (creator instanceof JSONArray array && !array.isEmpty()) {
            appendLine(strBui, "作者", array.getString(0));
            return;
        }
        String creatorText = creator.toString();
        try {
            List<?> list = JSONObject.parseObject(creatorText, List.class);
            if (!list.isEmpty()) {
                appendLine(strBui, "作者", String.valueOf(list.getFirst()));
                return;
            }
        } catch (Exception ignored) {
            // creator can also be a plain string.
        }
        appendLine(strBui, "作者", creatorText);
    }

    private void appendExtUrls(StringBuilder strBui, SearchItem result, JSONObject data) {
        JSONArray extUrls = data == null ? null : data.getJSONArray("ext_urls");
        if (extUrls != null) {
            for (Object extUrl : extUrls) {
                appendLine(strBui, "地址", String.valueOf(extUrl));
            }
            return;
        }
        Object extraUrls = result.getExtra().get("ext_urls");
        if (extraUrls instanceof JSONArray array) {
            for (Object extUrl : array) {
                appendLine(strBui, "地址", String.valueOf(extUrl));
            }
        }
    }

    private String stringExtra(SearchItem result, String key) {
        Object value = result.getExtra().get(key);
        return value == null ? null : String.valueOf(value);
    }

    private String firstNonBlank(String first, String second) {
        return StringUtils.isNotBlank(first) ? first : second;
    }

    private void appendLine(StringBuilder strBui, String label, String value) {
        if (StringUtils.isNotBlank(value)) {
            strBui.append(MessageFormat.format("{0}：{1}\n", label, value));
        }
    }
}
