package com.haruhi.botserver.features.imagesearch.handler;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
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
 * AnimeTrace 按识别区域返回角色与作品信息，无图片链接
 */
@Component
public class AnimeTraceImageSearchProvider implements ImageSearchProvider {

    private static final int MAX_OTHER_CHARACTERS = 3;

    private final PicImageSearchFactory picImageSearchFactory;

    public AnimeTraceImageSearchProvider(PicImageSearchFactory picImageSearchFactory) {
        this.picImageSearchFactory = picImageSearchFactory;
    }

    @Override
    public ImageSearchProviderType type() {
        return ImageSearchProviderType.ANIME_TRACE;
    }

    @Override
    public void search(Bot bot, Message message, Message replyMessage, String imageUrl) {
        SearchResponse searchResponse = picImageSearchFactory.animeTrace().search(SearchInput.byUrl(imageUrl));
        Object errorMessage = searchResponse.getExtra().get("message");
        if (errorMessage != null && StringUtils.isNotBlank(String.valueOf(errorMessage))) {
            bot.sendMessage(message.getUserId(), message.getGroupId(), message.getMessageType(),
                    MessageHolder.instanceText("AnimeTrace：" + errorMessage));
            return;
        }
        List<SearchItem> results = searchResponse.getRaw();
        if (CollectionUtils.isEmpty(results)) {
            bot.sendMessage(message.getUserId(), message.getGroupId(), message.getMessageType(),
                    MessageHolder.instanceText("AnimeTrace：未搜索到结果"));
            return;
        }
        List<ForwardMsgItem> forwardMsgs = new ArrayList<>();
        for (int i = 0; i < results.size(); i++) {
            forwardMsgs.add(ForwardMsgItem.instance(message.getSelfId(), bot.getBotName(),
                    MessageHolder.instanceText(getItemMsg(results.get(i), i + 1, results.size()))));
        }
        bot.sendForwardMessage(message.getUserId(), message.getGroupId(), message.getMessageType(), forwardMsgs);
    }

    private String getItemMsg(SearchItem result, int index, int total) {
        StringBuilder strBui = new StringBuilder();
        if (total > 1) {
            strBui.append("识别区域 ").append(index).append('/').append(total).append('\n');
        }
        strBui.append("角色：").append(result.getTitle()).append('\n');
        strBui.append("作品：").append(result.getSource());
        JSONObject origin = result.getOrigin() instanceof JSONObject json ? json : null;
        if (origin != null && origin.getBooleanValue("not_confident")) {
            strBui.append("\n可能不准确");
        }
        String otherCharacters = otherCharacters(origin, result.getTitle());
        if (StringUtils.isNotBlank(otherCharacters)) {
            strBui.append("\n其他候选：").append(otherCharacters);
        }
        return strBui.toString();
    }

    private String otherCharacters(JSONObject origin, String title) {
        JSONArray characters = origin == null ? null : origin.getJSONArray("character");
        if (characters == null) {
            return "";
        }
        List<String> others = new ArrayList<>();
        for (int i = 1; i < characters.size() && others.size() < MAX_OTHER_CHARACTERS; i++) {
            JSONObject character = characters.getJSONObject(i);
            String name = character.getString("character");
            if (StringUtils.isBlank(name) || name.equals(title)) {
                continue;
            }
            String work = character.getString("work");
            others.add(StringUtils.isBlank(work) ? name : name + "（" + work + "）");
        }
        return String.join("、", others);
    }
}
