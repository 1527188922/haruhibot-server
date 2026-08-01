package com.haruhi.botServer.handler.message.image;

import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSONObject;
import com.haruhi.botServer.constant.DictionaryEnum;
import com.haruhi.botServer.constant.ThirdPartyURL;
import com.haruhi.botServer.dto.qqclient.ForwardMsgItem;
import com.haruhi.botServer.dto.qqclient.Message;
import com.haruhi.botServer.dto.qqclient.MessageHolder;
import com.haruhi.botServer.dto.saucenao.Results;
import com.haruhi.botServer.service.DictionarySqliteService;
import com.haruhi.botServer.ws.Bot;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class SauceNaoImageSearchProvider implements ImageSearchProvider {

    private static final int SEARCH_TIMEOUT = 30 * 1000;

    private final DictionarySqliteService dictionarySqliteService;

    public SauceNaoImageSearchProvider(DictionarySqliteService dictionarySqliteService) {
        this.dictionarySqliteService = dictionarySqliteService;
    }

    @Override
    public ImageSearchProviderType type() {
        return ImageSearchProviderType.SAUCENAO;
    }

    @Override
    public void search(Bot bot, Message message, Message replyMessage, String imageUrl) {
        String apiKey = dictionarySqliteService.getInCache(DictionaryEnum.SAUCENAO_SEARCH_IMAGE__KEY.getKey(), null);

        Map<String, Object> param = new HashMap<>();
        param.put("output_type", 2);
        param.put("api_key", apiKey);
        param.put("testmode", 1);
        param.put("numres", 6);
        param.put("db", 99);
        param.put("url", imageUrl);

        log.info("开始请求搜图接口 图片:{}", imageUrl);
        try (HttpResponse response = HttpUtil.createPost(ThirdPartyURL.SEARCH_IMAGE).timeout(SEARCH_TIMEOUT).form(param).execute()) {
            log.debug("识图接口响应 {}", response);
            String body;
            if (response == null || !response.isOk() || StringUtils.isBlank(body = response.body())) {
                bot.sendMessage(message.getUserId(), message.getGroupId(), message.getMessageType(), "搜图异常", true);
                return;
            }
            JSONObject jsonObject = JSONObject.parseObject(body);
            String resultsStr = jsonObject.getString("results");
            if (Strings.isBlank(resultsStr)) {
                bot.sendMessage(message.getUserId(), message.getGroupId(), message.getMessageType(), "搜索结果为空", true);
                return;
            }
            List<Results> resultList = JSONObject.parseArray(resultsStr, Results.class);
            resultList.sort(Comparator.comparingDouble(this::similarity).reversed());
            sendResult(bot, resultList, message);
        } catch (Exception e) {
            bot.sendMessage(message.getUserId(), message.getGroupId(), message.getMessageType(), "搜图异常：" + e.getMessage(), true);
            log.error("搜图异常", e);
        }
    }

    private double similarity(Results results) {
        if (results == null || results.getHeader() == null || results.getHeader().getSimilarity() == null) {
            return 0D;
        }
        return results.getHeader().getSimilarity();
    }

    private void sendResult(Bot bot, List<Results> resultList, Message message) {
        List<ForwardMsgItem> forwardMsgs = new ArrayList<>();
        for (Results results : resultList) {
            forwardMsgs.add(ForwardMsgItem.instance(message.getSelfId(), bot.getBotName(), MessageHolder.instanceText(getItemMsg(results))));
        }
        bot.sendForwardMessage(message.getUserId(), message.getGroupId(), message.getMessageType(), forwardMsgs);
    }

    private String getItemMsg(Results results) {
        StringBuilder strBui = new StringBuilder();
        if (results.getHeader().getSimilarity() != null) {
            strBui.append(MessageFormat.format("相似度：{0}\n", results.getHeader().getSimilarity() + "%"));
        }
        if (results.getData().getTitle() != null) {
            strBui.append(MessageFormat.format("标题：{0}\n", results.getData().getTitle()));
        }
        if (results.getData().getSource() != null) {
            strBui.append(MessageFormat.format("来源：{0}\n", results.getData().getSource()));
        }
        if (results.getHeader().getIndex_name() != null) {
            strBui.append(MessageFormat.format("数据来源：{0}\n", results.getHeader().getIndex_name()));
        }
        if (results.getData().getJp_name() != null) {
            strBui.append(MessageFormat.format("日语名：{0}\n", results.getData().getJp_name()));
        }
        if (results.getData().getMaterial() != null) {
            strBui.append(MessageFormat.format("出处：{0}\n", results.getData().getMaterial()));
        }
        String pixivId = results.getData().getPixiv_id();
        if (pixivId != null) {
            strBui.append(MessageFormat.format("pid：{0}\n", pixivId));
        }
        if (results.getData().getMember_name() != null) {
            strBui.append(MessageFormat.format("作者：{0}\n", results.getData().getMember_name()));
        }
        String creator = results.getData().getCreator();
        if (creator != null) {
            List list;
            try {
                list = JSONObject.parseObject(creator, List.class);
                if (!list.isEmpty()) {
                    strBui.append(MessageFormat.format("作者：{0}\n", list.getFirst()));
                }
            } catch (Exception e) {
                strBui.append(MessageFormat.format("作者：{0}\n", creator));
            }
        }
        if (results.getData().getTwitter_user_id() != null) {
            strBui.append(MessageFormat.format("twitter作者id：{0}\n", results.getData().getTwitter_user_id()));
        }
        String[] extUrls = results.getData().getExt_urls();
        if (extUrls != null) {
            for (String extUrl : extUrls) {
                strBui.append(MessageFormat.format("地址：{0}\n", extUrl));
            }
        }
        if (results.getHeader().getThumbnail() != null) {
            strBui.append(MessageFormat.format("缩略图：{0}\n", results.getHeader().getThumbnail()));
        }
        if (pixivId != null) {
            strBui.append(MessageFormat.format("原图链接：https://pixiv.re/{0}.jpg", pixivId));
        }
        return strBui.toString();
    }
}
