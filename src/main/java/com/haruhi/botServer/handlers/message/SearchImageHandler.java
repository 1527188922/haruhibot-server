package com.haruhi.botServer.handlers.message;

import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSONObject;
import com.haruhi.botServer.cache.CacheSet;
import com.haruhi.botServer.constant.HandlerWeightEnum;
import com.haruhi.botServer.constant.RegexEnum;
import com.haruhi.botServer.constant.ThirdPartyURL;
import com.haruhi.botServer.dto.qqclient.ForwardMsgItem;
import com.haruhi.botServer.dto.qqclient.Message;
import com.haruhi.botServer.dto.qqclient.MessageData;
import com.haruhi.botServer.dto.qqclient.MessageHolder;
import com.haruhi.botServer.dto.saucenao.Results;
import com.haruhi.botServer.event.message.IAllMessageEvent;
import com.haruhi.botServer.service.DictionarySqliteService;
import com.haruhi.botServer.utils.ThreadPoolUtil;
import com.haruhi.botServer.ws.Bot;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class SearchImageHandler implements IAllMessageEvent {
    
    @Override
    public int weight() {
        return HandlerWeightEnum.W_760.getWeight();
    }

    @Override
    public String funName() {
        return HandlerWeightEnum.W_760.getName();
    }
    private static int size = 20;

    private static CacheSet<String> cache = new CacheSet<>(30,TimeUnit.SECONDS,size);

    private String getKey(String selfId,String userId,String groupId){
        return  selfId + "-" + userId + "-" + groupId;
    }

    @Autowired
    private DictionarySqliteService dictionarySqliteService;

    @Override
    public boolean onMessage(final Bot bot, final Message message) {

        boolean searchImageAllowGroup = dictionarySqliteService.getBoolean(DictionarySqliteService.DictionaryEnum.SWITCH_SEARCH_IMAGE_ALLOW_GROUP.getKey(), false);
        if(!searchImageAllowGroup && message.isGroupMsg()){
            return false;
        }

        Message replyMessage = replySearch(bot, message);
        if(replyMessage != null){
            // 回复式识图
            startSearch(bot,message,replyMessage,replyMessage.getPicUrls().get(0),null);
            return true;
        }

        List<MessageData> picMessageData = message.getPicMessageData();
        String key = getKey(String.valueOf(message.getSelfId()), String.valueOf(message.getUserId()), String.valueOf(message.getGroupId()));
        if(cache.contains(key) && CollectionUtils.isNotEmpty(picMessageData)){
            // 存在缓存 并且 图片路径不为空
            startSearch(bot,message,null,picMessageData.get(0).getUrl(),key);
            return true;
        }
        boolean matches = matches(message);

        if (matches) {
            if(CollectionUtils.isEmpty(picMessageData)){
                cache.add(key);
                bot.sendMessage(message.getUserId(),message.getGroupId(),message.getMessageType(),"图呢！",true);
            }else {
                startSearch(bot,message,null,picMessageData.get(0).getUrl(),key);
            }
            return true;
        }
        return false;
    }

    private boolean matches(Message message){
        List<String> texts = message.getTexts();
        if(CollectionUtils.isEmpty(texts)){
            return false;
        }
        String msg = texts.get(0).trim();
        String[] split = RegexEnum.SEARCH_IMAGE.getValue().split("\\|");
        for (String s : split) {
            if(s.equals(msg)){
                return true;
            }
        }
        return false;
    }

    private void startSearch(Bot bot,Message message, Message replyMessage,String url, String key){
        bot.sendMessage(message.getUserId(),message.getGroupId(),message.getMessageType(),"开始搜图...",true);
        ThreadPoolUtil.getHandleCommandPool().execute(()->{
//            KQCodeUtils instance = KQCodeUtils.getInstance();
//            String imageUrl = instance.getParam(this.cq, "url",CqCodeTypeEnum.image.getType(),0);

            String apiKey = dictionarySqliteService.getInCache(DictionarySqliteService.DictionaryEnum.SAUCENAO_SEARCH_IMAGE__KEY.getKey(), null);

            Map<String,Object> param = new HashMap<>();
            param.put("output_type",2);
            param.put("api_key",apiKey);
            param.put("testmode",1);
            param.put("numres",6);
            param.put("db",99);
            param.put("url", url);
//            param.add("file",new FileSystemResource(new File(picUrl)));

            log.info("开始请求搜图接口,图片:{}", url);
            try (HttpResponse response = HttpUtil.createPost(ThirdPartyURL.SEARCH_IMAGE).timeout(30 * 1000).form(param).execute()){
                log.debug("识图接口响应 {}",response);
                String body = null;
                if(response != null && response.isOk() && StringUtils.isNotBlank(body = response.body())) {
                    JSONObject jsonObject = JSONObject.parseObject(body);
                    String resultsStr = jsonObject.getString("results");
                    if(Strings.isBlank(resultsStr)){
                        bot.sendMessage(message.getUserId(),message.getGroupId(),message.getMessageType(), "搜索结果为空",true);
                    }else{
                        List<Results> resultList = JSONObject.parseArray(resultsStr, Results.class);
                        sort(resultList);
                        sendResult(bot,resultList, replyMessage,message);
                    }
                }
            }catch (Exception e){
                bot.sendMessage(message.getUserId(),message.getGroupId(),message.getMessageType(), "搜图异常："+e.getMessage(),true);
                log.error("搜图异常",e);
            }
        });
        if(key != null){
            cache.remove(key);
        }
    }
    private Message replySearch(Bot bot, Message message){
        if (message.isReplyMsg() && message.isTextMsg()) {
            if (message.getText(0).trim().matches(RegexEnum.SEARCH_IMAGE.getValue())) {
                List<String> replyMsgIds = message.getReplyMsgIds();
                Message msg = bot.getMsg(replyMsgIds.get(0),2L * 1000L).getData();
                log.debug("回复式识图，根据msgId获取消息 {} {}",replyMsgIds.get(0), JSONObject.toJSONString(msg));
                if(msg != null && msg.isPicMsg()){
                    return msg;
                }
            }
        }
        return null;
    }

    private void sort(List<Results> resultList){
        int size = resultList.size();
        for (int i = 0; i < size - 1; i++) {
            boolean flag = false;
            for (int f = 0; f < size - i - 1; f++) {
                if(resultList.get(f).getHeader().getSimilarity() < resultList.get(f + 1).getHeader().getSimilarity()){
                    Results results = resultList.get(f);
                    resultList.set(f,resultList.get(f + 1));
                    resultList.set(f + 1,results);
                    flag = true;
                }
            }
            if(!flag){
                break;
            }
        }
    }

    private void sendResult(Bot bot, List<Results> resultList, Message replyMessage, Message message){
        List<ForwardMsgItem> forwardMsgs = new ArrayList<>();
        for (Results results : resultList) {
            String itemMsg = getItemMsg(results);
            forwardMsgs.add(ForwardMsgItem.instance(message.getSelfId(), bot.getBotName(), MessageHolder.instanceText(itemMsg)));
        }
        bot.sendForwardMessage(message.getUserId(), message.getGroupId(), message.getMessageType(), forwardMsgs);
    }

//    private void sendResult(Bot bot, List<Results> resultList, Message replyMessage, Message message){
//        List<String> forwardMsgs = new ArrayList<>(resultList.size() + 1);
//        String m = replyMessage != null ? replyMessage.getRawMessage() : message.getRawMessage();
//        String cq = KQCodeUtils.getInstance().getCq(m, CqCodeTypeEnum.image.getType());
//        forwardMsgs.add(cq);
//        for (Results results : resultList) {
//            forwardMsgs.add(getItemMsg(results));
//        }
//
//        SyncResponse<String> syncResponse = bot.sendSyncMessage(message.getUserId(), message.getGroupId(), message.getMessageType(),
//                message.getSelfId(), BotConfig.NAME, forwardMsgs, 8 * 1000);
//        if(syncResponse == null || (syncResponse.getRetcode() != null && syncResponse.getRetcode() != 0)){
//            log.error("识图结果同步发送失败，使用异步发送");
//            forwardMsgs.remove(0);
//            bot.sendMessage(message.getUserId(), message.getGroupId(), message.getMessageType(), message.getSelfId(), BotConfig.NAME, forwardMsgs);
//        }
//    }
    private String getItemMsg(Results results){
        StringBuilder strBui = new StringBuilder();
        if(results.getHeader().getSimilarity() != null){
            strBui.append(MessageFormat.format("相似度：{0}\n",results.getHeader().getSimilarity()+"%"));
        }
        if(results.getData().getTitle() != null){
            strBui.append(MessageFormat.format("标题：{0}\n",results.getData().getTitle()));
        }
        if(results.getData().getSource() != null){
            strBui.append(MessageFormat.format("来源：{0}\n",results.getData().getSource()));
        }
        if(results.getHeader().getIndex_name() != null){
            strBui.append(MessageFormat.format("数据来源：{0}\n",results.getHeader().getIndex_name()));
        }
        if(results.getData().getJp_name() != null){
            strBui.append(MessageFormat.format("日语名：{0}\n",results.getData().getJp_name()));
        }
        if(results.getData().getMaterial() != null){
            strBui.append(MessageFormat.format("出处：{0}\n",results.getData().getMaterial()));
        }
        String pixivId = results.getData().getPixiv_id();
        if(pixivId != null){
            strBui.append(MessageFormat.format("pid：{0}\n",pixivId));
        }
        if(results.getData().getMember_name() != null){
            strBui.append(MessageFormat.format("作者：{0}\n",results.getData().getMember_name()));
        }
        String creator = results.getData().getCreator();
        if(creator != null){
            List list;
            try{
                list = JSONObject.parseObject(creator, List.class);
                if(list.size() > 0){
                    strBui.append(MessageFormat.format("作者：{0}\n",list.get(0)));
                }
            }catch (Exception e){
                strBui.append(MessageFormat.format("作者：{0}\n",creator));
            }
        }
        if(results.getData().getTwitter_user_id() != null){
            strBui.append(MessageFormat.format("twitter作者id：{0}\n",results.getData().getTwitter_user_id()));
        }
        String[] ext_urls = results.getData().getExt_urls();
        if(ext_urls != null && ext_urls.length > 0){
            for (String ext_url : ext_urls) {
                strBui.append(MessageFormat.format("地址：{0}\n",ext_url));
            }
        }
        if(results.getHeader().getThumbnail() != null){
            strBui.append(MessageFormat.format("缩略图：{0}\n",results.getHeader().getThumbnail()));
        }
        if(pixivId != null){
            strBui.append(MessageFormat.format("原图链接：https://pixiv.re/{0}.jpg",pixivId));
        }
        return strBui.toString();
    }
}
