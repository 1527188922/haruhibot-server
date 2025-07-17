package com.haruhi.botServer.service;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.haruhi.botServer.dto.bilibili.BilibiliBaseResp;
import com.haruhi.botServer.dto.bilibili.PlayUrlInfo;
import com.haruhi.botServer.dto.bilibili.VideoDetail;
import com.haruhi.botServer.utils.BilibiliIdConverter;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class BilibiliVideoParseService{

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
        return sendGetRequest("https://api.bilibili.com/x/web-interface/wbi/view/detail", param,  new TypeReference<BilibiliBaseResp<VideoDetail>>(){});
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


    /**
     * bvid avid 2传1即可
     * @param bvid
     * @param avid
     * @param cid
     * @return
     */
    public BilibiliBaseResp<PlayUrlInfo> getPlayUrlInfo(String bvid, Long avid, Long cid){
        HashMap<String, Object> param = new HashMap<String, Object>() {{
            put("bvid", bvid);
            put("avid", avid);
            put("cid", cid);
            put("qn", 127);
            put("otype", "json");
            put("fnver", 0);
            put("from_client", "BROWSER");
            put("is_main_page", false);
            put("need_fragment", false);
            put("isGaiaAvoided", true);
            put("web_location", 1315873);
            put("voice_balance", 1);
//            put("fnval", 1024);
        }};
        return sendGetRequest("https://api.bilibili.com/x/player/wbi/playurl", param, new TypeReference<BilibiliBaseResp<PlayUrlInfo>>(){});
    }


    public <T> BilibiliBaseResp<T> sendGetRequest(String url, HashMap<String, Object> urlParam, TypeReference<BilibiliBaseResp<T>> responseType){
        String s = HttpUtil.urlWithForm(url, urlParam, StandardCharsets.UTF_8, false);

        Map<String, String> headers = getHeaders(true);
        HttpRequest httpRequest = HttpRequest.get(s).addHeaders(headers);
        try (HttpResponse execute = httpRequest.execute()){
            if (execute.getStatus() != HttpStatus.HTTP_OK) {
                log.error("请求b站接口失败 url:{} status:{} body:{}", s, execute.getStatus(), execute.body());
                return null;
            }
            String body = execute.body();
            BilibiliBaseResp<T> bilibiliBaseResp = JSONObject.parseObject(body,responseType);
            bilibiliBaseResp.setRaw(body);
            return bilibiliBaseResp;
        }
    }

    public void downloadVideo(String url, File file,int timeout){
        HttpRequest httpRequest = HttpUtil.createGet(url, true)
                .addHeaders(getHeaders(false))
                .timeout(timeout);
        try (HttpResponse response = httpRequest.executeAsync()){
            response.writeBody(file, null);
        }
    }


    @Data
    @AllArgsConstructor
    public static class ParseResult {
        private String url;
        private String videoId;
        private String pageNum;

        private String matchedKeyword;
    }
}
