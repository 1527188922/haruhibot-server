package com.haruhi.botServer.service;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.HttpUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.haruhi.botServer.constant.BusinessModuleEnum;
import com.haruhi.botServer.constant.DictionaryEnum;
import com.haruhi.botServer.constant.ThirdPartyURL;
import com.haruhi.botServer.dto.bilibili.*;
import com.haruhi.botServer.utils.BilibiliIdConverter;
import com.haruhi.botServer.utils.BilibililSidUtil;
import com.haruhi.botServer.utils.DbLog;
import com.haruhi.botServer.utils.XMLUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class BilibiliService {

    private static final String BILIBILI_NAV_URL = "https://api.bilibili.com/x/web-interface/nav";
    private static final long WBI_KEY_CACHE_MILLIS = 10 * 60 * 1000L;
    private static final int[] WBI_MIXIN_KEY_ENC_TAB = {
            46, 47, 18, 2, 53, 8, 23, 32,
            15, 50, 10, 31, 58, 3, 45, 35,
            27, 43, 5, 49, 33, 9, 42, 19,
            29, 28, 14, 39, 12, 38, 41, 13,
            37, 48, 7, 16, 24, 55, 40, 61,
            26, 17, 0, 1, 60, 51, 30, 4,
            22, 25, 54, 21, 56, 59, 6, 63,
            57, 62, 11, 36, 20, 34, 44, 52
    };

    public static final Map<String, String> HEADERS = new HashMap<String, String>() {{
        put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36 Edg/138.0.0.0");
        put("Referer", "https://www.bilibili.com/");
        put("Accept", "application/json, text/plain, */*");
        put("Accept-Language", "zh-CN,zh;q=0.9");
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

    @Autowired
    private DictionarySqliteService dictionarySqliteService;

    private volatile String wbiMixinKey;
    private volatile long wbiMixinKeyExpireAt;


    /**
     * 获取视频详情
     * @param bvid
     * @return
     */
    public BilibiliBaseResp<VideoDetail> getVideoDetail(String bvid){
        HashMap<String, Object> param = new HashMap<String, Object>() {{
            put("bvid", bvid);
        }};
        return sendGetRequest("https://api.bilibili.com/x/web-interface/wbi/view/detail", param,  new TypeReference<BilibiliBaseResp<VideoDetail>>(){});
    }
    /**
     * 获取视频下载链接
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


    /**
     * 通过视频bv获取cid
     * 无需认证
     * @param bv
     * @return
     */
    public PlayerInfoResp getPlayerInfo(String bv){
        Map<String, Object> param = new HashMap<>();
        param.put("bvid",bv);
        param.put("jsonp","jsonp");

        String s = HttpUtil.urlWithForm(ThirdPartyURL.PLAYER_CID, param, StandardCharsets.UTF_8, false);
        HttpRequest httpRequest = HttpUtil.createGet(s)
                .timeout(10 * 1000);
        try (HttpResponse response = httpRequest.execute()){
            BilibiliBaseResp<List<PlayerInfoResp>> listBilibiliBaseResp = JSONObject.parseObject(response.body(), new TypeReference<BilibiliBaseResp<List<PlayerInfoResp>>>() {
            });
            return Objects.nonNull(listBilibiliBaseResp) && CollectionUtils.isNotEmpty(listBilibiliBaseResp.getData())
                    ? listBilibiliBaseResp.getData().getFirst() : null;
        }
    }



    /**
     * 根据视频cid获取弹幕
     * @param cid
     * @return
     */
    public BulletChatResp getChatList(Long cid){
        Map<String, Object> param = new HashMap<>();
        param.put("oid",cid);

        String s = HttpUtil.urlWithForm(ThirdPartyURL.BULLET_CHAR, param, StandardCharsets.UTF_8, false);
        HttpRequest httpRequest = HttpUtil.createGet(s).timeout(10 * 1000);
        try (HttpResponse response = httpRequest.execute()){
            return XMLUtil.convertXmlToObject(BulletChatResp.class, response.body());
        }
    }

    public String getCookie(){
        String sessdata = dictionarySqliteService.getInCache(DictionaryEnum.BILIBILI_COOKIES_SESSDATA.getKey(), null);
        String jct = dictionarySqliteService.getInCache(DictionaryEnum.BILIBILI_COOKIES_BILI_JCT.getKey(), null);
        if(StringUtils.isBlank(sessdata) || StringUtils.isBlank(jct)){
            return null;
        }
        ArrayList<String> strings = new ArrayList<>();
        strings.add("SESSDATA=" + sessdata);
        strings.add("bili_jct=" + jct);
        strings.add("b_lsid=" + BilibililSidUtil.generate());
        BilibiliTickResp tick = this.getTick(null);
        if (tick != null) {
            strings.add("bili_ticket=" + tick.getTicket());
            Long expires = tick.expires();
            if (expires != null) {
                strings.add("bili_ticket_expires=" + expires);
            }
        }
        return StringUtils.join(strings,"; ");
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
        text = text.replace("\\/","/");
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
            String cookieValue = getCookie();
            if (StringUtils.isNotBlank(cookieValue)) {
                map.put("Cookie", cookieValue);
            }
        }
        return map;
    }

    /**
     * Convert a byte array to a hex string.
     *
     * @param bytes The byte array to convert.
     * @return The hex string representation of the given byte array.
     */
    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                sb.append('0');
            }
            sb.append(hex);
        }
        return sb.toString();
    }

    /**
     * Generate a HMAC-SHA256 hash of the given message string using the given key
     * string.
     *
     * @param key     The key string to use for the HMAC-SHA256 hash.
     * @param message The message string to hash.
     * @throws Exception If an error occurs during the HMAC-SHA256 hash generation.
     * @return The HMAC-SHA256 hash of the given message string using the given key
     *         string.
     */
    public static String hmacSha256(String key, String message) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKeySpec);
        byte[] hash = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(hash);
    }

    /**
     * 优先从db获取ticket
     * @param csrf
     * @return
     */
    public synchronized BilibiliTickResp getTick(String csrf){
        BilibiliTickResp tickInDb = this.getTickInDb();
        if (tickInDb != null && !tickInDb.expired() && StringUtils.isNotBlank(tickInDb.getTicket())) {
            return tickInDb;
        }
        try {
            BilibiliBaseResp<BilibiliTickResp> ticketBaseResp = this.genTicket(csrf);
            if (ticketBaseResp == null || !ticketBaseResp.isSuccess()) {
                return null;
            }
            dictionarySqliteService.add(DictionaryEnum.BILIBILI_COOKIES_TICKET.getKey(), ticketBaseResp.getRaw());
            return ticketBaseResp.getData();
        } catch (Exception e) {
            return null;
        }
    }
    public BilibiliTickResp getTickInDb(){
        String s = dictionarySqliteService.get(DictionaryEnum.BILIBILI_COOKIES_TICKET.getKey());
        try {
            if (StringUtils.isNotBlank(s)) {
                BilibiliBaseResp<BilibiliTickResp> tickResp = JSONObject.parseObject(s, new TypeReference<BilibiliBaseResp<BilibiliTickResp>>() {
                });
                return tickResp.getData();
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public BilibiliBaseResp<BilibiliTickResp> genTicket(String csrf) throws Exception {
        long ts = System.currentTimeMillis() / 1000;
        String hexSign = hmacSha256("XgwSnGZ1p", "ts" + ts);
        Map<String, Object> param = new HashMap<>(){{
            put("csrf", csrf == null ? "" : csrf);
            put("key_id", "ec02");
            put("hexsign", hexSign);
            put("context[ts]", ts);
        }};
        String s = HttpUtil.urlWithForm("https://api.bilibili.com/bapis/bilibili.api.ticket.v1.Ticket/GenWebTicket",
                param,
                StandardCharsets.UTF_8,
                false);

        HttpRequest request = HttpRequest.post(s).addHeaders(getHeaders(false));
        try (HttpResponse response = request.execute()){
            String body = response.body();
            BilibiliBaseResp<BilibiliTickResp> tickResp = JSONObject.parseObject(body, new TypeReference<BilibiliBaseResp<BilibiliTickResp>>() {
            });
            tickResp.setRaw(body);
            if (tickResp.isSuccess()) {
                return tickResp;
            }
            DbLog.error(BusinessModuleEnum.BILIBILI,"请求b站ticket响应失败 url:{} resp:{}",s,body);
            return tickResp;
        } catch (Exception e) {
            DbLog.error(BusinessModuleEnum.BILIBILI,"请求b站ticket异常 url:{}",s,e);
            return null;
        }
    }

    public static void main(String[] args) {
        BilibiliService bilibiliService = new BilibiliService();
        try {
//            BilibiliBaseResp<BilibiliTickResp> bilibiliTickRespBilibiliBaseResp = bilibiliService.genTicket(null);
//            System.out.println(bilibiliTickRespBilibiliBaseResp.getRaw());
            System.out.println(BilibililSidUtil.generate());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public <T> BilibiliBaseResp<T> sendGetRequest(String url, HashMap<String, Object> urlParam, TypeReference<BilibiliBaseResp<T>> responseType){
//        boolean wbiApi = url.contains("/wbi/");
        String s = buildUrl(url, urlParam, false, false);

        Map<String, String> headers = getHeaders(true);
        HttpRequest httpRequest = HttpRequest.get(s).addHeaders(headers);
        try (HttpResponse execute = httpRequest.execute()){
            if (execute.getStatus() != HttpStatus.HTTP_OK) {
                DbLog.error(BusinessModuleEnum.BILIBILI,
                        "请求b站接口失败 url:{} status:{} body:{}", s, execute.getStatus(), execute.body());
                return null;
            }
            String body = execute.body();
            BilibiliBaseResp<T> bilibiliBaseResp = JSONObject.parseObject(body,responseType);
            if (!bilibiliBaseResp.isSuccess()) {
                DbLog.error(BusinessModuleEnum.BILIBILI,"b站接口响应异常 url:{} body:{}", s, body);
            }else {
                DbLog.info(BusinessModuleEnum.BILIBILI,"b站接口响应 url:{} body:{}", s, body);
            }
            bilibiliBaseResp.setRaw(body);
            return bilibiliBaseResp;
        }
    }

    private String buildUrl(String url, Map<String, Object> urlParam, boolean wbiSign, boolean refreshWbiKey) {
        if (!wbiSign) {
            return HttpUtil.urlWithForm(url, urlParam, StandardCharsets.UTF_8, false);
        }
        Map<String, Object> signedParam = signWbiParams(urlParam, refreshWbiKey);
        if (signedParam == null) {
            return HttpUtil.urlWithForm(url, urlParam, StandardCharsets.UTF_8, false);
        }
        return url + "?" + buildQueryString(signedParam);
    }

    private Map<String, Object> signWbiParams(Map<String, Object> params, boolean refreshWbiKey) {
        TreeMap<String, Object> signedParam = new TreeMap<>();
        if (params != null) {
            params.entrySet().stream()
                    .filter(entry -> Objects.nonNull(entry.getValue()))
                    .forEach(entry -> signedParam.put(entry.getKey(), entry.getValue()));
        }
        signedParam.put("wts", System.currentTimeMillis() / 1000);

        String query = buildQueryString(signedParam);
        String mixinKey = getWbiMixinKey(refreshWbiKey);
        if (StringUtils.isBlank(mixinKey)) {
            return null;
        }
        signedParam.put("w_rid", DigestUtil.md5Hex(query + mixinKey));
        return signedParam;
    }

    private String buildQueryString(Map<String, Object> params) {
        StringBuilder query = new StringBuilder();
        params.entrySet().stream()
                .filter(entry -> Objects.nonNull(entry.getValue()))
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    if (!query.isEmpty()) {
                        query.append("&");
                    }
                    query.append(encodeURIComponent(entry.getKey()))
                            .append("=")
                            .append(encodeURIComponent(cleanWbiValue(entry.getValue())));
                });
        return query.toString();
    }

    private String cleanWbiValue(Object value) {
        return String.valueOf(value).replaceAll("[!'()*]", "");
    }

    private String encodeURIComponent(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8)
                .replace("+", "%20")
                .replace("%7E", "~");
    }

    private String getWbiMixinKey(boolean refresh) {
        long now = System.currentTimeMillis();
        if (!refresh && StringUtils.isNotBlank(wbiMixinKey) && now < wbiMixinKeyExpireAt) {
            return wbiMixinKey;
        }
        synchronized (this) {
            now = System.currentTimeMillis();
            if (!refresh && StringUtils.isNotBlank(wbiMixinKey) && now < wbiMixinKeyExpireAt) {
                return wbiMixinKey;
            }
            String mixinKey = fetchWbiMixinKey();
            if (StringUtils.isNotBlank(mixinKey)) {
                wbiMixinKey = mixinKey;
                wbiMixinKeyExpireAt = now + WBI_KEY_CACHE_MILLIS;
            }
            return wbiMixinKey;
        }
    }

    private String fetchWbiMixinKey() {
        try (HttpResponse response = HttpRequest.get(BILIBILI_NAV_URL)
                .addHeaders(getHeaders(true))
                .timeout(10 * 1000)
                .execute()) {
            if (response.getStatus() != HttpStatus.HTTP_OK) {
                log.error("获取b站wbi key失败 status:{} body:{}", response.getStatus(), response.body());
                return null;
            }
            JSONObject body = JSONObject.parseObject(response.body());
            JSONObject wbiImg = body.getJSONObject("data").getJSONObject("wbi_img");
            String imgKey = getWbiImageKey(wbiImg.getString("img_url"));
            String subKey = getWbiImageKey(wbiImg.getString("sub_url"));
            String rawKey = imgKey + subKey;
            StringBuilder mixinKey = new StringBuilder();
            for (int index : WBI_MIXIN_KEY_ENC_TAB) {
                mixinKey.append(rawKey.charAt(index));
            }
            return mixinKey.substring(0, 32);
        } catch (Exception e) {
            log.error("获取b站wbi key异常", e);
            return null;
        }
    }

    private String getWbiImageKey(String url) {
        int slashIndex = url.lastIndexOf("/");
        int dotIndex = url.lastIndexOf(".");
        return url.substring(slashIndex + 1, dotIndex);
    }

    public void downloadVideo(String url, File file,int timeout){
        HttpRequest httpRequest = HttpUtil.createGet(url, true)
                .addHeaders(getHeaders(false))
                .timeout(timeout);
        try (HttpResponse response = httpRequest.execute()){
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
