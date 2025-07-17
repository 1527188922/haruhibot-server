package com.haruhi.botServer.service;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.haruhi.botServer.dto.bilibili.BilibiliBaseResp;
import com.haruhi.botServer.dto.bilibili.VideoDetail;
import com.haruhi.botServer.utils.BilibiliIdConverter;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class BilibiliVideoParseService implements CommandLineRunner {

    public static final Map<String, String> HEADERS = new HashMap<String, String>() {{
        put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36 Edg/138.0.0.0");
        put("Referer", "https://www.bilibili.com");
    }};


    // 定义正则表达式模式
    public static final Map<String, Pattern> PATTERNS = new HashMap<String, Pattern>() {{
        put("BV", Pattern.compile("(BV[1-9a-zA-Z]{10})(?:\\s)?(\\d{1,3})?"));
        put("av", Pattern.compile("av(\\d{6,})(?:\\s)?(\\d{1,3})?"));
        put("/BV", Pattern.compile("/(BV[1-9a-zA-Z]{10})()"));
        put("/av", Pattern.compile("/av(\\d{6,})()"));
        put("b23", Pattern.compile("https?://b23\\.tv/[A-Za-z\\d\\._?%&+\\-=/#]+()()"));
        put("bili2233", Pattern.compile("https?://bili2233\\.cn/[A-Za-z\\d\\._?%&+\\-=/#]+()()"));
        put("bilibili", Pattern.compile("https?://(?:space|www|live|m|t)?\\.?bilibili\\.com/[A-Za-z\\d\\._?%&+\\-=/#]+()()"));
    }};

    @Value("${bilibili.SESSDATA:}")
    private String sessdata;

    @Value("${bilibili.bili_jct:}")
    private String jct;


    public BilibiliBaseResp<VideoDetail> getVideoDetail(String bvid){
        HashMap<String, Object> param = new HashMap<String, Object>() {{
            put("bvid", bvid);
        }};
        String s = HttpUtil.urlWithForm("https://api.bilibili.com/x/web-interface/wbi/view/detail", param, StandardCharsets.UTF_8, false);

        Map<String, String> headers = getHeaders(true);
        HttpRequest httpRequest = HttpRequest.get(s).addHeaders(headers);
        try (HttpResponse execute = httpRequest.execute()){
            if (execute.getStatus() != HttpStatus.HTTP_OK) {
                log.error("获取b站视频详情失败 status:{} body:{}", execute.getStatus(), execute.body());
                return null;
            }
            String body = execute.body();
            BilibiliBaseResp<VideoDetail> videoDetailBilibiliBaseResp = JSONObject.parseObject(body, new TypeReference<BilibiliBaseResp<VideoDetail>>() {
            });
            videoDetailBilibiliBaseResp.setRaw(body);
            return videoDetailBilibiliBaseResp;
        }
    }

    public String getCookie(){
        if(StringUtils.isBlank(sessdata) || StringUtils.isBlank(jct)){
            return null;
        }
        ArrayList<String> strings = new ArrayList<>();
        strings.add("SESSDATA="+sessdata);
        strings.add("bili_jct="+jct);
        return StringUtils.join(strings,";");
    }

    public String getBvidInText(String text){
        ParseResult parseResult = parse(text);
        if (parseResult == null) {
            return null;
        }
        log.info("消息匹配bilibili正则 {}",JSONObject.toJSONString(parseResult));
        String matchedKeyword = parseResult.getMatchedKeyword();

        String bvid = "";
        if (Arrays.asList("b23","bili2233").contains(matchedKeyword)) {
            String redirectUrl = getRedirectUrl(parseResult.getUrl());
            bvid = parse(redirectUrl).getVideoId();
        }else if(Arrays.asList("BV","/BV").contains(matchedKeyword)){
            bvid = parseResult.getVideoId();
        }else if(Arrays.asList("av","/av").contains(matchedKeyword)){
            try {
                String videoId = parseResult.getVideoId().trim();
                String lowerCase = videoId.toLowerCase();
                String aid = null;
                if (lowerCase.startsWith("av")) {
                    aid = lowerCase.replaceFirst("av", "");
                }else{
                    aid = lowerCase;
                }
                bvid = BilibiliIdConverter.aid2bvid(Long.parseLong(aid));
            } catch (Exception e) {
                log.error("从av获取bv异常 text: {}", text);
            }
        }
        return bvid;
    }

    public ParseResult parse(String text) {
        String matchedKeyword = matchKeyword(text);
        if (matchedKeyword != null) {
            Matcher matcher = PATTERNS.get(matchedKeyword).matcher(text);
            if (matcher.find()) {
                String url = matcher.group(0);
                String videoId = matcher.group(1);
                String pageNum = matcher.groupCount() >= 2 ? matcher.group(2) : null;
                return new ParseResult(url, videoId, pageNum, matchedKeyword);
            }
        }
        return null;
    }

    public String matchKeyword(String text){
        return PATTERNS.keySet().stream()
                .filter(key -> PATTERNS.get(key).matcher(text).find())
                .findFirst().orElse(null);
    }

    public String getRedirectUrl(String url){
        HttpRequest httpRequest = HttpUtil.createGet(url).setFollowRedirects(false)
                .addHeaders(getHeaders(false));
        try (HttpResponse execute = httpRequest.execute()){
            if (execute.getStatus() >= HttpStatus.HTTP_BAD_REQUEST) {
                return null;
            }
            return execute.header("Location");
        }
    }

    public Map<String, String> getHeaders(boolean cookie) {
        HashMap<String, String> map = new HashMap<>(HEADERS);
        if (cookie) {
            map.put("Cookie", getCookie());
        }
        return map;
    }

    @Override
    public void run(String... args) throws Exception {
        String testText = "[CQ:json,data={\"ver\":\"1.0.0.19\"&#44;\"prompt\":\"&#91;QQ小程序&#93;【红牛极限运动】原来这才是真正的漂流，贵阳通天河，这才是真正的极限漂流！\"&#44;\"config\":{\"type\":\"normal\"&#44;\"width\":0&#44;\"height\":0&#44;\"forward\":1&#44;\"autoSize\":0&#44;\"ctime\":1752719442&#44;\"token\":\"25a642386b682de9d93bf7480e13c3c2\"}&#44;\"needShareCallBack\":false&#44;\"app\":\"com.tencent.miniapp_01\"&#44;\"view\":\"view_8C8E89B49BE609866298ADDFF2DBABA4\"&#44;\"meta\":{\"detail_1\":{\"appid\":\"1109937557\"&#44;\"appType\":0&#44;\"title\":\"哔哩哔哩\"&#44;\"desc\":\"【红牛极限运动】原来这才是真正的漂流，贵阳通天河，这才是真正的极限漂流！\"&#44;\"icon\":\"https:\\/\\/open.gtimg.cn\\/open\\/app_icon\\/00\\/95\\/17\\/76\\/100951776_100_m.png?t=1752651105\"&#44;\"preview\":\"i0.hdslb.com\\/bfs\\/share_ttl\\/e6f8cc59ab993602e30ed5cb57fe4a1fff7eb738.jpg\"&#44;\"url\":\"m.q.qq.com\\/a\\/s\\/c25aad5aa1b38c1ca62d226afe989624\"&#44;\"scene\":1036&#44;\"host\":{\"uin\":1527188922&#44;\"nick\":\"　\"}&#44;\"shareTemplateId\":\"8C8E89B49BE609866298ADDFF2DBABA4\"&#44;\"shareTemplateData\":{}&#44;\"qqdocurl\":\"https://b23.tv/dHosECf?share_medium=android&amp;share_source=qq&amp;bbid=XU57D845FA319BA7D7BF53965FFDBDC5934F4&amp;ts=1752719442753\"&#44;\"showLittleTail\":\"\"&#44;\"gamePoints\":\"\"&#44;\"gamePointsUrl\":\"\"&#44;\"shareOrigin\":0}}}]";

//        String testText = "https://www.bilibili.com/video/BV1FKuxzmECV?buvid=XU57D845FA319BA7D7BF53965FFDBDC5934F4&from_spmid=main.ugc-video-detail-vertical.0.0&is_story_h5=false&mid=Gw9dFuSUUmKAJXBKTU0QVw%3D%3D&p=1&plat_id=116&share_from=ugc&share_medium=android&share_plat=android&share_session_id=fd630242-e1c1-4243-8168-61e4bade8311&share_source=QQ&share_tag=s_i&spmid=united.player-video-detail.0.0&timestamp=1752719441&unique_k=dHosECf&up_id=3494350363298639";
        // 查找匹配的关键字
        String bvid = getBvidInText(testText);
        BilibiliBaseResp<VideoDetail> videoDetail = getVideoDetail(bvid);
        System.out.println(videoDetail);
    }


    @Data
    @AllArgsConstructor
    public static class ParseResult {
        private String url;
        private String videoId;
        private String pageNum;

        private String matchedKeyword;
    }


    public static void main(String[] args) {
        String testText = "[CQ:json,data={\"ver\":\"1.0.0.19\"&#44;\"prompt\":\"&#91;QQ小程序&#93;【红牛极限运动】原来这才是真正的漂流，贵阳通天河，这才是真正的极限漂流！\"&#44;\"config\":{\"type\":\"normal\"&#44;\"width\":0&#44;\"height\":0&#44;\"forward\":1&#44;\"autoSize\":0&#44;\"ctime\":1752719442&#44;\"token\":\"25a642386b682de9d93bf7480e13c3c2\"}&#44;\"needShareCallBack\":false&#44;\"app\":\"com.tencent.miniapp_01\"&#44;\"view\":\"view_8C8E89B49BE609866298ADDFF2DBABA4\"&#44;\"meta\":{\"detail_1\":{\"appid\":\"1109937557\"&#44;\"appType\":0&#44;\"title\":\"哔哩哔哩\"&#44;\"desc\":\"【红牛极限运动】原来这才是真正的漂流，贵阳通天河，这才是真正的极限漂流！\"&#44;\"icon\":\"https:\\/\\/open.gtimg.cn\\/open\\/app_icon\\/00\\/95\\/17\\/76\\/100951776_100_m.png?t=1752651105\"&#44;\"preview\":\"i0.hdslb.com\\/bfs\\/share_ttl\\/e6f8cc59ab993602e30ed5cb57fe4a1fff7eb738.jpg\"&#44;\"url\":\"m.q.qq.com\\/a\\/s\\/c25aad5aa1b38c1ca62d226afe989624\"&#44;\"scene\":1036&#44;\"host\":{\"uin\":1527188922&#44;\"nick\":\"　\"}&#44;\"shareTemplateId\":\"8C8E89B49BE609866298ADDFF2DBABA4\"&#44;\"shareTemplateData\":{}&#44;\"qqdocurl\":\"https://b23.tv/dHosECf?share_medium=android&amp;share_source=qq&amp;bbid=XU57D845FA319BA7D7BF53965FFDBDC5934F4&amp;ts=1752719442753\"&#44;\"showLittleTail\":\"\"&#44;\"gamePoints\":\"\"&#44;\"gamePointsUrl\":\"\"&#44;\"shareOrigin\":0}}}]";

//        String testText = "https://www.bilibili.com/video/BV1FKuxzmECV?buvid=XU57D845FA319BA7D7BF53965FFDBDC5934F4&from_spmid=main.ugc-video-detail-vertical.0.0&is_story_h5=false&mid=Gw9dFuSUUmKAJXBKTU0QVw%3D%3D&p=1&plat_id=116&share_from=ugc&share_medium=android&share_plat=android&share_session_id=fd630242-e1c1-4243-8168-61e4bade8311&share_source=QQ&share_tag=s_i&spmid=united.player-video-detail.0.0&timestamp=1752719441&unique_k=dHosECf&up_id=3494350363298639";
        // 查找匹配的关键字
        BilibiliVideoParseService bilibiliVideoParseService = new BilibiliVideoParseService();
        String bvid = bilibiliVideoParseService.getBvidInText(testText);
        System.out.println(bvid);
    }
}
