package com.haruhi.botServer.service.news;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import com.haruhi.botServer.constant.ThirdPartyURL;
import com.haruhi.botServer.dto.news163.NewsResp;
import com.haruhi.botServer.dto.qqclient.MessageHolder;
import com.haruhi.botServer.utils.DateTimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Slf4j
@Service
public class NewsServiceImpl implements NewsService {

    @Override
    public List<NewsResp> requestNewsBy163(){
        log.info("开始获取网易新闻...");
        long l = System.currentTimeMillis();
        String sourceId = "T1348647853363";
        HttpRequest httpRequest = HttpUtil.createGet(ThirdPartyURL.NEWS_163).timeout(2 * 1000);
        try (HttpResponse response = httpRequest.execute()){
            String responseStr = response.body();
            if (Strings.isBlank(responseStr)) {
                return null;
            }
            JSONObject responseJson = JSONObject.parseObject(responseStr);
            JSONArray jsonArray = responseJson.getJSONArray(sourceId);
            if(CollectionUtils.isEmpty(jsonArray)){
                return null;
            }
            List<NewsResp> newsBy163Resps = JSONArray.parseArray(jsonArray.toJSONString(), NewsResp.class);
            newsBy163Resps = newsBy163Resps.stream().collect(
                    Collectors.collectingAndThen(Collectors.toCollection(()-> new TreeSet<>(Comparator.comparing(NewsResp::getPostid))), ArrayList::new)
            ).stream().sorted(Comparator.comparing(NewsResp::getLmodify).reversed()).collect(Collectors.toList());
            log.info("获取网易新闻完成,耗时:{}",System.currentTimeMillis() - l);
            return newsBy163Resps;
        }catch (Exception e){
            log.error("获取网易新闻异常",e);
            return null;
        }
    }

    @Override
    public List<List<MessageHolder>> createNewsMessage(List<NewsResp> list){
        List<List<MessageHolder>> forwardMsgs = new ArrayList<>(list.size() + 1);
        forwardMsgs.add(MessageHolder.instanceText("今日新闻"));

        for (NewsResp e : list) {
            List<MessageHolder> newsItemMessage = createNewsItemMessage(e);
            forwardMsgs.add(newsItemMessage);
        }
        return forwardMsgs;
    }

    private List<MessageHolder> createNewsItemMessage(NewsResp e){


        String stringBuilder = "【" + e.getTitle() + "】\n[" +
                DateTimeUtil.dateTimeFormat(e.getLmodify(), DateTimeUtil.PatternEnum.yyyyMMddHHmmss) + "]\n" +
                e.getDigest();

        List<MessageHolder> messageHolders = MessageHolder.instanceText(stringBuilder);

        if(Strings.isNotBlank(e.getImgsrc())){
            String imgsrc = e.getImgsrc().replaceFirst("http,","http:");
            messageHolders.add(MessageHolder.instanceImage(imgsrc));
        }

        StringBuilder stringBuilder2 = new StringBuilder();
        if(Strings.isNotBlank(e.getUrl())){
            stringBuilder2.append("详情:").append(e.getUrl());
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
                stringBuilder2.append("详情:").append(e.getUrl());
            }
        }
        messageHolders.addAll(MessageHolder.instanceText(stringBuilder2.toString()));
        return messageHolders;
    }
}
