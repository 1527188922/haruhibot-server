package com.haruhi.botServer.service.news;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import com.haruhi.botServer.config.BotConfig;
import com.haruhi.botServer.constant.CqCodeTypeEnum;
import com.haruhi.botServer.constant.ThirdPartyURL;
import com.haruhi.botServer.dto.news.response.NewsBy163Resp;
import com.haruhi.botServer.utils.CommonUtil;
import com.haruhi.botServer.utils.DateTimeUtil;
import com.haruhi.botServer.utils.RestUtil;
import com.haruhi.botServer.ws.Server;
import com.simplerobot.modules.utils.KQCodeUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.socket.WebSocketSession;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Slf4j
@Service
public class NewsServiceImpl implements NewsService {

    @Override
    public List<NewsBy163Resp> requestNewsBy163(){
        log.info("开始获取网易新闻...");
        long l = System.currentTimeMillis();
        String sourceId = "T1348647853363";
        try {
            String responseStr = RestUtil.sendGetRequest(RestUtil.getRestTemplate(2 * 1000), ThirdPartyURL.NEWS_163, null, String.class);
            if (Strings.isNotBlank(responseStr)) {
                JSONObject responseJson = JSONObject.parseObject(responseStr);
                JSONArray jsonArray = responseJson.getJSONArray(sourceId);
                if(!CollectionUtils.isEmpty(jsonArray)){
                    List<NewsBy163Resp> newsBy163Resps = JSONArray.parseArray(jsonArray.toJSONString(), NewsBy163Resp.class);
                    newsBy163Resps = newsBy163Resps.stream().collect(
                            Collectors.collectingAndThen(Collectors.toCollection(()-> new TreeSet<>(Comparator.comparing(NewsBy163Resp::getPostid))), ArrayList::new)
                    ).stream().sorted(Comparator.comparing(NewsBy163Resp::getLmodify).reversed()).collect(Collectors.toList());
                    log.info("获取网易新闻完成,耗时:{}",System.currentTimeMillis() - l);
                    return newsBy163Resps;
                }
            }
            return null;
        }catch (Exception e){
            log.error("获取网易新闻异常",e);
            return null;
        }
    }

    @Override
    public void sendGroup(WebSocketSession session, List<NewsBy163Resp> list, Long... groupIds) {
        if(groupIds != null){
            List<String> newsGroupMessage = createNewsGroupMessage(list);
            for (Long groupId : groupIds) {
                Server.sendGroupMessage(session,groupId,BotConfig.NAME,newsGroupMessage);
            }
        }
    }

    private List<String> createNewsGroupMessage(List<NewsBy163Resp> list){
        List<String> forwardMsgs = new ArrayList<>(list.size() + 1);
        KQCodeUtils instance = KQCodeUtils.getInstance();
        forwardMsgs.add("今日新闻");
        for (NewsBy163Resp e : list) {
            String newsItemMessage = createNewsItemMessage(e, instance);
            forwardMsgs.add(newsItemMessage);
        }
        return forwardMsgs;
    }
    private String createNewsItemMessage(NewsBy163Resp e,KQCodeUtils instance){
        StringBuilder stringBuilder = new StringBuilder("【");
        stringBuilder.append(e.getTitle()).append("】\n[");
        stringBuilder.append(DateTimeUtil.dateTimeFormat(e.getLmodify(), DateTimeUtil.PatternEnum.yyyyMMddHHmmss)).append("]\n");
        stringBuilder.append(e.getDigest()).append("\n");
        if(Strings.isNotBlank(e.getImgsrc())){
            String cq = instance.toCq(CqCodeTypeEnum.image.getType(), "file=" + e.getImgsrc());
            stringBuilder.append(cq).append("\n");
        }
        if(Strings.isNotBlank(e.getUrl())){
            stringBuilder.append("详情:").append(e.getUrl());
        }else{
            boolean hasUrl = false;
            if(Strings.isNotBlank(e.getPostid())){
                hasUrl = true;
                e.setUrl("https://3g.163.com/dy/article/" + e.getPostid() + ".html");
            }else if(Strings.isNotBlank(e.getDocid())){
                hasUrl = true;
                e.setUrl("https://3g.163.com/dy/article/" + e.getDocid() + ".html");
            }
            if(hasUrl){
                stringBuilder.append("详情:").append(e.getUrl());
            }
        }
        return stringBuilder.toString();
    }
    private String createNewsPrivateMessage(List<NewsBy163Resp> list){
        StringBuilder stringBuilder = new StringBuilder();
        KQCodeUtils instance = KQCodeUtils.getInstance();
        for (NewsBy163Resp e : list) {
            stringBuilder.append(createNewsItemMessage(e,instance)).append("\n");
        }
        return stringBuilder.toString();
    }

    @Override
    public void sendPrivate(WebSocketSession session,List<NewsBy163Resp> list,Long... userIds) {
        if(userIds != null){
            List<List<NewsBy163Resp>> lists = CommonUtil.averageAssignList(list, 10);
            for (Long userId : userIds) {
                for (List<NewsBy163Resp> newsBy163Resps : lists) {
                    Server.sendPrivateMessage(session,userId,createNewsPrivateMessage(newsBy163Resps),false);
                }
            }
        }

    }
}
