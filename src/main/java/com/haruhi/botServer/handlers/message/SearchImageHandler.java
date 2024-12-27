package com.haruhi.botServer.handlers.message;

import com.alibaba.fastjson.JSONObject;
import com.haruhi.botServer.cache.CacheSet;
import com.haruhi.botServer.config.BotConfig;
import com.haruhi.botServer.config.SwitchConfig;
import com.haruhi.botServer.constant.CqCodeTypeEnum;
import com.haruhi.botServer.constant.HandlerWeightEnum;
import com.haruhi.botServer.constant.RegexEnum;
import com.haruhi.botServer.constant.ThirdPartyURL;
import com.haruhi.botServer.dto.gocq.response.Message;
import com.haruhi.botServer.dto.gocq.response.SyncResponse;
import com.haruhi.botServer.dto.searchImage.response.Results;
import com.haruhi.botServer.event.message.IAllMessageEvent;
import com.haruhi.botServer.utils.ThreadPoolUtil;
import com.haruhi.botServer.utils.RestUtil;
import com.haruhi.botServer.ws.Bot;
import com.simplerobot.modules.utils.KQCodeUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.ResourceAccessException;

import java.net.SocketTimeoutException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
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
    @Override
    public boolean onMessage(final Bot bot, final Message message) {

        if(!SwitchConfig.SEARCH_IMAGE_ALLOW_GROUP && message.isGroupMsg()){
            return false;
        }

        Message replyMessage = replySearch(bot, message);
        if(replyMessage != null){
            // 回复式识图
            startSearch(bot,message,replyMessage,replyMessage.getPicUrls().get(0),null);
            return true;
        }

        List<Message.MessageData> picMessageData = message.getPicMessageData();
        String key = getKey(String.valueOf(message.getSelfId()), String.valueOf(message.getUserId()), String.valueOf(message.getGroupId()));
        if(cache.contains(key) && !CollectionUtils.isEmpty(picMessageData)){
            // 存在缓存 并且 图片路径不为空
            startSearch(bot,message,null,picMessageData.get(0).getUrl(),key);
            return true;
        }

        boolean matches = false;
        String[] split = RegexEnum.SEARCH_IMAGE.getValue().split("\\|");
        for (String s : split) {
            if(message.getRawMessage().startsWith(s)){
                matches = true;
                break;
            }
        }
        if (matches) {
            if(CollectionUtils.isEmpty(picMessageData)){
                cache.add(key);
                bot.sendMessage(message.getUserId(),message.getGroupId(),message.getMessageType(),"图呢！",true);
            }else if(!CollectionUtils.isEmpty(picMessageData)){
                startSearch(bot,message,null,picMessageData.get(0).getUrl(),key);
            }
            return true;
        }
        return false;
    }

    private void startSearch(Bot bot,Message message, Message replyMessage,String url, String key){
        bot.sendMessage(message.getUserId(),message.getGroupId(),message.getMessageType(),"开始搜图...",true);
        ThreadPoolUtil.getHandleCommandPool().execute(new SearchImageTask(bot,message,replyMessage,url));
        if(key != null){
            cache.remove(key);
        }
    }
    private Message replySearch(Bot bot, Message message){
        if (message.isGroupMsg() && message.isReplyMsg()) {
            String s = message.getRawMessage().replaceAll(RegexEnum.CQ_CODE_REPLACR.getValue(), "").trim();
            if (s.matches(RegexEnum.SEARCH_IMAGE.getValue())) {
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

    @AllArgsConstructor
    private class SearchImageTask implements Runnable{
        private Bot bot;
        private Message message;
        private Message replyMessage;
        private String url;
        
        
        @Override
        public void run() {
//            KQCodeUtils instance = KQCodeUtils.getInstance();
//            String imageUrl = instance.getParam(this.cq, "url",CqCodeTypeEnum.image.getType(),0);

            LinkedMultiValueMap<String,Object> param = new LinkedMultiValueMap<>(6);
            param.add("output_type",2);
            param.add("api_key",BotConfig.SEARCH_IMAGE_KEY);
            param.add("testmode",1);
            param.add("numres",6);
            param.add("db",99);
            param.add("url", url);
//            param.add("file",new FileSystemResource(new File(picUrl)));
            try {
                log.info("开始请求搜图接口,图片:{}", url);
//                String response = RestUtil.sendPostForm(RestUtil.getRestTemplate(30 * 1000), ThirdPartyURL.SEARCH_IMAGE, param, String.class);
                ResponseEntity<String> response = RestUtil.sendPostForm(RestUtil.getRestTemplate(30 * 1000), ThirdPartyURL.SEARCH_IMAGE, param,
                        null, null, new ParameterizedTypeReference<String>() {});
                log.debug("识图接口响应 {}",response);
                if(response.getBody() != null){
                    JSONObject jsonObject = JSONObject.parseObject(response.getBody());
                    String resultsStr = jsonObject.getString("results");
                    if(Strings.isBlank(resultsStr)){
                        bot.sendMessage(message.getUserId(),message.getGroupId(),message.getMessageType(), "搜索结果为空",true);
                    }else{
                        List<Results> resultList = JSONObject.parseArray(resultsStr, Results.class);
                        sort(resultList);
                        sendResult(bot,resultList, replyMessage,message);
                    }
                }
            }catch (ResourceAccessException e){
                Throwable cause = e.getCause();
                if(cause instanceof SocketTimeoutException){
                    bot.sendMessage(message.getUserId(),message.getGroupId(),message.getMessageType(), "搜图超时",true);
                    log.error("搜图超时",e);
                }else{
                    bot.sendMessage(message.getUserId(),message.getGroupId(),message.getMessageType(), "搜图异常："+e.getMessage(),true);
                    log.error("搜图异常",e);
                }
            }catch (Exception e){
                bot.sendMessage(message.getUserId(),message.getGroupId(),message.getMessageType(), "搜图异常："+e.getMessage(),true);
                log.error("搜图异常",e);
            }

        }
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
        List<String> forwardMsgs = new ArrayList<>(resultList.size() + 1);
        String m = replyMessage != null ? replyMessage.getRawMessage() : message.getRawMessage();
        String cq = KQCodeUtils.getInstance().getCq(m, CqCodeTypeEnum.image.getType());
        forwardMsgs.add(cq);
        for (Results results : resultList) {
            forwardMsgs.add(getItemMsg(results));
        }

        SyncResponse<String> syncResponse = bot.sendSyncMessage(message.getUserId(), message.getGroupId(), message.getMessageType(),
                message.getSelfId(), BotConfig.NAME, forwardMsgs, 8 * 1000);
        if(syncResponse == null || (syncResponse.getRetcode() != null && syncResponse.getRetcode() != 0)){
            log.error("识图结果同步发送失败，使用异步发送");
            forwardMsgs.remove(0);
            bot.sendMessage(message.getUserId(), message.getGroupId(), message.getMessageType(), message.getSelfId(), BotConfig.NAME, forwardMsgs);
        }
    }
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
