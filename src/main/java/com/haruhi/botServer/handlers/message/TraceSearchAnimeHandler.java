package com.haruhi.botServer.handlers.message;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.haruhi.botServer.cache.CacheSet;
import com.haruhi.botServer.constant.HandlerWeightEnum;
import com.haruhi.botServer.constant.RegexEnum;
import com.haruhi.botServer.dto.qqclient.ForwardMsgItem;
import com.haruhi.botServer.dto.qqclient.Message;
import com.haruhi.botServer.dto.qqclient.MessageData;
import com.haruhi.botServer.dto.qqclient.MessageHolder;
import com.haruhi.botServer.dto.trace.SearchResp;
import com.haruhi.botServer.event.message.IAllMessageEvent;
import com.haruhi.botServer.utils.CommonUtil;
import com.haruhi.botServer.utils.ThreadPoolUtil;
import com.haruhi.botServer.ws.Bot;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 根据图片搜索番剧
 */
@Slf4j
@Component
public class TraceSearchAnimeHandler implements IAllMessageEvent {

    public static final String TRACE_API = "https://api.trace.moe/search?cutBorders";
    private static final int SEARCH_TIMEOUT = 10 * 1000;

    private static int size = 20;

    private static CacheSet<String> cache = new CacheSet<>(30, TimeUnit.SECONDS,size);

    private String getKey(String selfId,String userId,String groupId){
        return  selfId + "-" + userId + "-" + groupId;
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
    public boolean onMessage(Bot bot, Message message) {
        Message replyMessage = replySearch(bot, message);
        if(replyMessage != null){
            startSearch(bot,message,replyMessage,replyMessage.getPicUrls().getFirst(),null);
            return true;
        }

        List<MessageData> picMessageData = message.getPicMessageData();
        String key = getKey(String.valueOf(message.getSelfId()), String.valueOf(message.getUserId()), String.valueOf(message.getGroupId()));
        if(cache.contains(key) && CollectionUtils.isNotEmpty(picMessageData)){
            // 存在缓存 并且 图片不为空
            startSearch(bot,message,null,picMessageData.getFirst().getUrl(),key);
            return true;
        }
        boolean matches = matches(message);

        if (matches) {
            if(CollectionUtils.isEmpty(picMessageData)){
                cache.add(key);
                bot.sendMessage(message.getUserId(),message.getGroupId(),message.getMessageType(),"图呢！",true);
            }else {
                startSearch(bot,message,null,picMessageData.getFirst().getUrl(),key);
            }
            return true;
        }
        return false;
    }


    private void startSearch(Bot bot,Message message, Message replyMessage,String imgUrl, String key){
        bot.sendMessage(message.getUserId(),message.getGroupId(),message.getMessageType(), MessageHolder.instanceText("开始搜番..."));
        ThreadPoolUtil.getHandleCommandPool().execute(()->{
            try {
                HashMap<String, Object> hashMap = new HashMap<>();
                hashMap.put("url", imgUrl);
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
                    List<ForwardMsgItem> forwardMsgItems = resultToForwardMsgItems(bot, message, result);
                    bot.sendForwardMessage(message.getUserId(), message.getGroupId(), message.getMessageType(), forwardMsgItems);
                }
            }catch (Exception e){
                log.error("搜番异常",e);
                bot.sendMessage(message.getUserId(),message.getGroupId(),message.getMessageType(), MessageHolder.instanceText("搜番异常："+e.getMessage()));
            }
        });
        if(key != null){
            cache.remove(key);
        }
    }

    public List<ForwardMsgItem> resultToForwardMsgItems(Bot bot,Message message,List<SearchResp.Result<Object>> results){
        List<ForwardMsgItem> forwardMsgItems = new ArrayList<>();
        for (SearchResp.Result<Object> result : results) {
            List<MessageHolder> messageHolders = new ArrayList<>();
            if (StringUtils.isNotBlank(result.getImage())) {
                MessageHolder imageMessageHolder = MessageHolder.instanceImage(result.getImage());
                messageHolders.add(imageMessageHolder);
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(result.getFilename());

            stringBuilder.append("\n");
            stringBuilder.append("相似度："+numberFormat(result.getSimilarity()));

            if (result.getEpisode() != null) {
                stringBuilder.append("\n");
                stringBuilder.append("第"+result.getEpisode()+"集"+ CommonUtil.formatDuration((long) result.getAt().floatValue(),TimeUnit.SECONDS));
            }else if(result.getAt() != null) {
                stringBuilder.append("\n");
                stringBuilder.append("第"+CommonUtil.formatDuration((long) result.getAt().floatValue(),TimeUnit.SECONDS));
            }
            if (StringUtils.isNotBlank(result.getVideo())) {
                stringBuilder.append("\n");
                stringBuilder.append("预览视频："+result.getVideo());
            }
            messageHolders.addAll(MessageHolder.instanceText(stringBuilder.toString()));
            forwardMsgItems.add(ForwardMsgItem.instance(message.getSelfId(), bot.getBotName(), messageHolders));
        }
        return forwardMsgItems;
    }

    private String numberFormat(Float num){
        NumberFormat nf = NumberFormat.getPercentInstance();
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        return nf.format(num);
    }

    public static void main(String[] args) {
//        String url = "https://api.trace.moe/search?cutBorders";
//        HashMap<String, Object> hashMap = new HashMap<>();
//        hashMap.put("url", "https://multimedia.nt.qq.com.cn/download?appid=1406&fileid=EhQctcPOEHlXXbF4t8fH6fZ1QrSvGRiQvQkg_goo08Cvivf_jgMyBHByb2RaEI6s3o-0OEh7497KuhS6jGl6ApMX&rkey=CAESMLCrySQnHsivbuzOz1Q3izKlI5bxSb5DTNqxUjlVOZzMtPQLaiZJE0DNq8v1eP541g");
//        String urlWithParams = HttpUtil.urlWithForm(url, hashMap, StandardCharsets.UTF_8, false);
//        HttpRequest httpRequest = HttpUtil.createGet(urlWithParams).timeout(10000);
//        try (HttpResponse response = httpRequest.execute()){
//            String body = response.body();
//            System.out.println(body);
//            SearchResp<Long> searchResp = JSONObject.parseObject(body, new TypeReference<SearchResp<Long>>() { });
//            System.out.println(searchResp.getError());
//        }

        Float f = 0.9717157287525382F;
        NumberFormat nf = NumberFormat.getPercentInstance();
        nf.setMinimumFractionDigits(2); // 固定两位小数
        nf.setMaximumFractionDigits(2);

        String result = nf.format(f);
        System.out.println(result); // 输出：85.36%
    }

    private Message replySearch(Bot bot, Message message){
        if (message.isReplyMsg() && message.isTextMsg()) {
            if (message.getText(0).trim().matches(RegexEnum.SEARCH_ANIME.getValue())) {
                List<String> replyMsgIds = message.getReplyMsgIds();
                Message msg = bot.getMsg(replyMsgIds.getFirst(),2L * 1000L).getData();
                log.debug("回复式识番，根据msgId获取消息 {} {}",replyMsgIds.getFirst(), JSONObject.toJSONString(msg));
                if(msg != null && msg.isPicMsg()){
                    return msg;
                }
            }
        }
        return null;
    }


    private boolean matches(Message message){
        List<String> texts = message.getTexts();
        if(CollectionUtils.isEmpty(texts)){
            return false;
        }
        String msg = texts.getFirst().trim();
        String[] split = RegexEnum.SEARCH_ANIME.getValue().split("\\|");
        for (String s : split) {
            if(s.equals(msg)){
                return true;
            }
        }
        return false;
    }
}
