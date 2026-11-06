package com.haruhi.botserver.features.bilibili.service;

import com.haruhi.botserver.configuration.metadata.ConfigKey;
import com.haruhi.botserver.configuration.service.Configs;

import cn.hutool.http.*;
import cn.hutool.core.io.StreamProgress;
import cn.hutool.crypto.digest.DigestUtil;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.haruhi.botserver.shared.constant.BusinessModuleEnum;
import com.haruhi.botserver.features.bilibili.client.model.bilibili.BilibiliBaseResp;
import com.haruhi.botserver.features.bilibili.client.model.bilibili.BilibiliTickResp;
import com.haruhi.botserver.features.bilibili.client.model.bilibili.BulletChatResp;
import com.haruhi.botserver.features.bilibili.client.model.bilibili.LiveStatusInfo;
import com.haruhi.botserver.features.bilibili.client.model.bilibili.PlayerInfoResp;
import com.haruhi.botserver.features.bilibili.client.model.bilibili.PlayUrlInfo;
import com.haruhi.botserver.features.bilibili.client.model.bilibili.VideoDetail;
import com.haruhi.botserver.features.bilibili.persistence.entity.BilibiliVideoSqlite;
import com.haruhi.botserver.features.bilibili.support.BilibiliIdConverter;
import com.haruhi.botserver.features.bilibili.support.BilibiliSidUtil;
import com.haruhi.botserver.infrastructure.logging.DbLog;
import com.haruhi.botserver.shared.util.XMLUtil;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class BilibiliService {

    private static final String BILIBILI_NAV_URL = "https://api.bilibili.com/x/web-interface/nav";
    private static final String BILIBILI_FINGER_SPI_URL = "https://api.bilibili.com/x/frontend/finger/spi";
    private static final String BILIBILI_VIEW_DETAIL_URL = "https://api.bilibili.com/x/web-interface/wbi/view/detail";
    /**
     * 老的未签名视频详情接口，wbi接口被风控时用于兜底
     */
    private static final String BILIBILI_VIEW_DETAIL_LEGACY_URL = "https://api.bilibili.com/x/web-interface/view";
    private static final String BILIBILI_PLAY_URL = "https://api.bilibili.com/x/player/wbi/playurl";
    private static final long WBI_KEY_CACHE_MILLIS = 10 * 60 * 1000L;
    private static final int REQUEST_TIMEOUT_MILLIS = 15 * 1000;
    /**
     * 命中风控(-412)后的冷却时长：冷却期内不再请求b站接口。
     * b站的-412是"客户端ip被服务端风控"，短时间内再次请求只会延长封禁，所以要主动退避
     */
    private static final long RISK_BLOCK_COOL_DOWN_MILLIS = 3 * 60 * 1000L;
    /**
     * 视频详情缓存时长，避免同一条视频被反复解析时重复请求b站
     */
    private static final long DETAIL_CACHE_MILLIS = 10 * 60 * 1000L;
    /**
     * 下载链接缓存时长（b站返回的url本身有有效期，不宜缓存太久）
     */
    private static final long PLAY_URL_CACHE_MILLIS = 10 * 60 * 1000L;
    private static final int CACHE_MAX_SIZE = 500;
    /**
     * 客户端ip被服务端风控，请求被拦截
     */
    private static final int CODE_RISK_BLOCKED = -412;
    /**
     * wbi签名校验失败（mixin key 过期或签名错误）
     */
    private static final int CODE_WBI_INVALID = -403;
    /**
     * b站被风控时HTTP状态码也是412（响应体是 {"code":-412,"message":"request was banned"}，不是业务json）
     */
    private static final int HTTP_STATUS_PRECONDITION_FAILED = 412;
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

    private volatile String wbiMixinKey;
    private volatile long wbiMixinKeyExpireAt;
    /**
     * 风控冷却截止时间戳（毫秒），小于等于当前时间表示未被风控
     */
    private volatile long riskBlockedUntil;
    /**
     * 设备标识（buvid3/buvid4）首次获取时只让一个线程去请求
     */
    private final Object deviceCookieLock = new Object();
    private final Map<String, CacheEntry<BilibiliBaseResp<VideoDetail>>> videoDetailCache = new ConcurrentHashMap<>();
    private final Map<String, CacheEntry<BilibiliBaseResp<PlayUrlInfo>>> playUrlCache = new ConcurrentHashMap<>();

    /**
     * 视频入库。直接 new BilibiliService() 的场景（如main方法）下为null
     */
    @Autowired(required = false)
    private BilibiliVideoSqliteService bilibiliVideoSqliteService;


    /**
     * 获取视频详情
     * <p>
     * 每次调用都会把响应按 bv号+cid 保存到 t_bilibili_video（存在则更新，不存在则插入）
     * <p>
     * 风控策略：
     * <ol>
     *     <li>先读缓存，10分钟内同一条视频不会重复请求</li>
     *     <li>请求走带wbi签名的接口，被风控(-412)时降级到老的未签名接口</li>
     *     <li>仍然被风控则进入冷却期，冷却期内直接返回失败响应，不再请求b站接口</li>
     * </ol>
     * @param bvid
     * @return
     */
    public BilibiliBaseResp<VideoDetail> getVideoDetail(String bvid){
        BilibiliBaseResp<VideoDetail> cached = getCache(videoDetailCache, bvid);
        if (Objects.nonNull(cached)) {
            DbLog.debug(BusinessModuleEnum.BILIBILI,"b站视频详情命中缓存 bvid:{}", bvid);
            return cached;
        }

        BilibiliBaseResp<VideoDetail> resp = requestVideoDetail(BILIBILI_VIEW_DETAIL_URL, bvid);
        if (isRiskBlocked(resp)) {
            DbLog.warn(BusinessModuleEnum.BILIBILI,"b站wbi视频详情接口被风控，降级到老接口重试 bvid:{} code:{}", bvid, codeOf(resp));
            resp = requestVideoDetail(BILIBILI_VIEW_DETAIL_LEGACY_URL, bvid);
        }

        if (Objects.nonNull(resp) && resp.isSuccess()) {
            putCache(videoDetailCache, bvid, resp, DETAIL_CACHE_MILLIS);
        }
        saveVideoDetail(bvid, resp);
        return resp;
    }

    /**
     * 请求视频详情；风控冷却期内直接返回被拦截的响应，不再发请求
     */
    private BilibiliBaseResp<VideoDetail> requestVideoDetail(String url, String bvid){
        if (inRiskCoolDown()) {
            return blockedResp("b站接口被风控，冷却中，不再请求");
        }
        HashMap<String, Object> param = new HashMap<String, Object>() {{
            put("bvid", bvid);
        }};
        BilibiliBaseResp<VideoDetail> resp = sendGetRequest(url, param, new TypeReference<BilibiliBaseResp<VideoDetail>>(){});
        return Objects.isNull(resp) ? blockedResp("请求b站视频详情接口异常") : resp;
    }

    /**
     * 视频详情入库，入库失败不影响接口返回
     */
    private void saveVideoDetail(String bvid, BilibiliBaseResp<VideoDetail> resp){
        if (Objects.isNull(bilibiliVideoSqliteService) || Objects.isNull(resp) || !resp.isSuccess() || Objects.isNull(resp.getData())) {
            return;
        }
        try {
            BilibiliVideoSqlite saved = bilibiliVideoSqliteService.saveOrUpdateByVideoDetail(resp.getData(), resp.getRaw());
            if (Objects.isNull(saved)) {
                // 响应成功但没有视频信息或缺少bv/cid时不会入库，只记录日志不抛异常
                DbLog.warn(BusinessModuleEnum.BILIBILI,"b站视频详情响应缺少视频信息，未入库 bvid:{} resp:{}", bvid, resp.getRaw());
            }
        } catch (Exception e) {
            DbLog.error(BusinessModuleEnum.BILIBILI,"b站视频详情入库异常 bvid:{}",bvid,e);
        }
    }

    /**
     * 获取视频下载链接
     * bvid avid 2传1即可
     * <p>
     * 调用后会把响应原始json更新到 t_bilibili_video.play_url_raw（记录不存在时不处理）
     * @param bvid
     * @param avid
     * @param cid
     * @return
     */
    public BilibiliBaseResp<PlayUrlInfo> getPlayUrlInfo(String bvid, Long avid, Long cid){
        String cacheKey = bvid + "_" + cid;
        BilibiliBaseResp<PlayUrlInfo> cached = getCache(playUrlCache, cacheKey);
        if (Objects.nonNull(cached)) {
            DbLog.debug(BusinessModuleEnum.BILIBILI,"b站视频下载链接命中缓存 bvid:{} cid:{}", bvid, cid);
            return cached;
        }
        if (inRiskCoolDown()) {
            return blockedResp("b站接口被风控，冷却中，不再请求");
        }

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
        BilibiliBaseResp<PlayUrlInfo> resp = sendGetRequest(BILIBILI_PLAY_URL, param, new TypeReference<BilibiliBaseResp<PlayUrlInfo>>(){});
        if (Objects.isNull(resp)) {
            return blockedResp("请求b站视频下载链接接口异常");
        }
        if (resp.isSuccess() && Objects.nonNull(cid)) {
            putCache(playUrlCache, cacheKey, resp, PLAY_URL_CACHE_MILLIS);
        }
        updatePlayUrlRaw(bvid, cid, resp);
        return resp;
    }

    /**
     * 保存下载链接接口的响应原始json
     */
    private void updatePlayUrlRaw(String bvid, Long cid, BilibiliBaseResp<PlayUrlInfo> resp){
        if (Objects.isNull(bilibiliVideoSqliteService) || Objects.isNull(resp) || StringUtils.isBlank(resp.getRaw())) {
            return;
        }
        try {
            bilibiliVideoSqliteService.updatePlayUrlRaw(bvid, cid, resp.getRaw());
        } catch (Exception e) {
            DbLog.error(BusinessModuleEnum.BILIBILI,"b站视频下载链接入库异常 bvid:{} cid:{}",bvid,cid,e);
        }
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

        String s = HttpUtil.urlWithForm(Configs.getStr(ConfigKey.URL_CONF_BILIBILI_PLAYER_CID), param, StandardCharsets.UTF_8, false);
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

        String s = HttpUtil.urlWithForm(Configs.getStr(ConfigKey.URL_CONF_BILIBILI_BULLET_CHAR), param, StandardCharsets.UTF_8, false);
        HttpRequest httpRequest = HttpUtil.createGet(s).timeout(10 * 1000);
        try (HttpResponse response = httpRequest.execute()){
            return XMLUtil.convertXmlToObject(BulletChatResp.class, response.body());
        }
    }

    /**
     * 单次请求最多查询的主播数量
     */
    public static final int LIVE_STATUS_UID_BATCH_SIZE = 100;

    /**
     * 根据uid批量获取直播间状态
     * 该接口会被定时任务高频调用，日志只输出到文件，不写库
     * @param uids b站主播uid
     * @return key: uid  value: 直播间状态；全部请求都失败时返回空map，查询失败的批次对应的uid不会出现在返回结果中
     */
    public Map<Long, LiveStatusInfo> getLiveStatusInfoByUids(Collection<Long> uids){
        if (CollectionUtils.isEmpty(uids)) {
            return Collections.emptyMap();
        }
        List<Long> uidList = new ArrayList<>(uids);
        Map<Long, LiveStatusInfo> result = new HashMap<>(uidList.size());
        for (int i = 0; i < uidList.size(); i += LIVE_STATUS_UID_BATCH_SIZE) {
            List<Long> batch = uidList.subList(i, Math.min(i + LIVE_STATUS_UID_BATCH_SIZE, uidList.size()));
            Map<Long, LiveStatusInfo> batchResult = requestLiveStatus(batch);
            if (batchResult != null) {
                result.putAll(batchResult);
            }
        }
        return result;
    }

    /**
     * 请求一次直播状态接口
     * @return 请求或响应异常返回null
     */
    private Map<Long, LiveStatusInfo> requestLiveStatus(List<Long> uids){
        JSONObject body = new JSONObject();
        body.put("uids", uids);

        String url = Configs.getStr(ConfigKey.URL_CONF_BILIBILI_LIVE_STATUS);
        HttpRequest httpRequest = HttpRequest.post(url)
                .contentType(ContentType.JSON.getValue())
                .body(body.toJSONString())
                .timeout(REQUEST_TIMEOUT_MILLIS);
        for (Map.Entry<String, String> entry : getLiveHeaders().entrySet()) {
            httpRequest.header(entry.getKey(), entry.getValue(), true);
        }
        try (HttpResponse response = httpRequest.execute()){
            String respBody = response.body();
            if (response.getStatus() != HttpStatus.HTTP_OK || StringUtils.isBlank(respBody)) {
                DbLog.error(BusinessModuleEnum.BILIBILI,"请求b站直播状态接口失败 url:{} status:{} body:{}",url,response.getStatus(),respBody);
                return null;
            }
            BilibiliBaseResp<Map<String, LiveStatusInfo>> resp = JSONObject.parseObject(respBody, new TypeReference<BilibiliBaseResp<Map<String, LiveStatusInfo>>>(){});
            if (resp == null || !resp.isSuccess() || resp.getData() == null) {
                log.error("b站直播状态接口响应异常 url:{} body:{}",url,respBody);
                return null;
            }
            DbLog.debug(BusinessModuleEnum.BILIBILI,"b站直播状态接口响应 url:{} body:{}",url,respBody);
            Map<Long, LiveStatusInfo> result = new HashMap<>(resp.getData().size());
            for (Map.Entry<String, LiveStatusInfo> entry : resp.getData().entrySet()) {
                try {
                    result.put(Long.parseLong(entry.getKey()), entry.getValue());
                }catch (NumberFormatException e){
                    DbLog.error(BusinessModuleEnum.BILIBILI,"b站直播状态接口返回的uid不是数字 uid:{}",entry.getKey());
                }
            }
            return result;
        }catch (Exception e){
            DbLog.error(BusinessModuleEnum.BILIBILI,"请求b站直播状态接口异常 url:{}",url,e);
            return null;
        }
    }

    /**
     * 请求b站直播接口的请求头
     */
    private Map<String, String> getLiveHeaders(){
        Map<String, String> headers = new HashMap<>(HEADERS);
//        String cookie = Configs.getStr(ConfigKey.BILIBILI_LIVE_COOKIE, ConfigKey.BILIBILI_LIVE_COOKIE.getDefaultValue());
//        if (StringUtils.isNotBlank(cookie)) {
//            headers.put("Cookie", cookie);
//        }
        return headers;
    }

    /**
     * 组装请求b站接口用的Cookie
     * <p>
     * 组成部分：{@code bilibili.cookies.raw}（浏览器整条cookie）或 {@code bilibili.cookies.sessdata} 登录态，
     * 加上程序维护的设备标识（buvid3/buvid4）与 bili_ticket。
     * <p>
     * 无论哪种模式，原始cookie里的 bili_ticket 都会被丢掉：它有3天有效期，浏览器里那份是快照且不会续期，
     * 统一改用程序生成、过期前自动刷新的那一份。
     * @return 不会返回null
     */
    public String getCookie(){
        Map<String, String> cookies = new LinkedHashMap<>();
        String raw = Configs.getStr(ConfigKey.BILIBILI_COOKIES_RAW, null);
        if (StringUtils.isNotBlank(raw)) {
            cookies.putAll(parseCookie(raw));
            // 浏览器里的ticket是快照且不会续期，统一换成程序维护的那份
            cookies.remove("bili_ticket");
            cookies.remove("bili_ticket_expires");
        } else {
            String sessdata = Configs.getStr(ConfigKey.BILIBILI_COOKIES_SESSDATA, null);
            if (StringUtils.isNotBlank(sessdata)) {
                cookies.put("SESSDATA", sessdata);
            }
            cookies.put("b_lsid", BilibiliSidUtil.generate());
        }
        fillDeviceCookies(cookies);
        appendTicket(cookies);
        List<String> parts = new ArrayList<>(cookies.size());
        for (Map.Entry<String, String> entry : cookies.entrySet()) {
            parts.add(entry.getKey() + "=" + entry.getValue());
        }
        return StringUtils.join(parts, "; ");
    }

    /**
     * 追加 bili_ticket：过期（提前5分钟）后由 {@link #getTick(String)} 自动重新生成，所以每次请求前都能拿到有效的
     */
    private void appendTicket(Map<String, String> cookies){
        BilibiliTickResp tick = this.getTick(null);
        if (tick != null && StringUtils.isNotBlank(tick.getTicket())) {
            cookies.put("bili_ticket", tick.getTicket());
            Long expires = tick.expires();
            if (expires != null) {
                cookies.put("bili_ticket_expires", String.valueOf(expires));
            }
        }
    }

    /**
     * 解析浏览器复制出来的整条cookie（{@code a=1; b=2}），容忍换行、多余空格和开头的 "Cookie:" 前缀
     */
    private Map<String, String> parseCookie(String raw){
        Map<String, String> cookies = new LinkedHashMap<>();
        String text = raw.replace("\r", " ").replace("\n", " ").trim();
        if (text.regionMatches(true, 0, "cookie:", 0, 7)) {
            text = text.substring(7);
        }
        for (String segment : text.split(";")) {
            String item = segment.trim();
            if (item.isEmpty()) {
                continue;
            }
            int index = item.indexOf('=');
            if (index <= 0) {
                continue;
            }
            cookies.put(item.substring(0, index).trim(), item.substring(index + 1).trim());
        }
        return cookies;
    }

    /**
     * 补齐设备标识。buvid3/buvid4 由程序从b站接口获取后写入配置，之后一直复用（同一个设备标识才像同一个浏览器）
     */
    private void fillDeviceCookies(Map<String, String> cookies){
        if (StringUtils.isNotBlank(cookies.get("buvid3"))) {
            return;
        }
        String buvid3 = Configs.getStr(ConfigKey.BILIBILI_COOKIES_BUVID3, null);
        String buvid4 = Configs.getStr(ConfigKey.BILIBILI_COOKIES_BUVID4, null);
        if (StringUtils.isBlank(buvid3) || StringUtils.isBlank(buvid4)) {
            synchronized (deviceCookieLock) {
                buvid3 = Configs.getStr(ConfigKey.BILIBILI_COOKIES_BUVID3, null);
                buvid4 = Configs.getStr(ConfigKey.BILIBILI_COOKIES_BUVID4, null);
                if (StringUtils.isBlank(buvid3) || StringUtils.isBlank(buvid4)) {
                    String[] device = fetchDeviceId();
                    if (device != null) {
                        buvid3 = device[0];
                        buvid4 = device[1];
                        Configs.save(ConfigKey.BILIBILI_COOKIES_BUVID3, buvid3);
                        Configs.save(ConfigKey.BILIBILI_COOKIES_BUVID4, buvid4);
                    }
                }
            }
        }
        if (StringUtils.isNotBlank(buvid3)) {
            cookies.put("buvid3", buvid3);
        }
        if (StringUtils.isNotBlank(buvid4) && StringUtils.isBlank(cookies.get("buvid4"))) {
            cookies.put("buvid4", buvid4);
        }
    }

    /**
     * 从b站获取一组设备标识
     * @return {buvid3, buvid4}，获取失败返回null
     */
    private String[] fetchDeviceId(){
        try (HttpResponse response = createGetRequest(BILIBILI_FINGER_SPI_URL, false).execute()){
            String body = response.body();
            if (response.getStatus() != HttpStatus.HTTP_OK || StringUtils.isBlank(body)) {
                DbLog.warn(BusinessModuleEnum.BILIBILI,"获取b站设备标识失败 status:{} body:{}", response.getStatus(), body);
                return null;
            }
            JSONObject json = JSONObject.parseObject(body);
            JSONObject data = json == null ? null : json.getJSONObject("data");
            if (data == null) {
                DbLog.warn(BusinessModuleEnum.BILIBILI,"获取b站设备标识响应异常 body:{}", body);
                return null;
            }
            String buvid3 = data.getString("b_3");
            String buvid4 = data.getString("b_4");
            if (StringUtils.isBlank(buvid3)) {
                return null;
            }
            DbLog.info(BusinessModuleEnum.BILIBILI,"获取b站设备标识成功 buvid3:{}", buvid3);
            return new String[]{buvid3, buvid4};
        } catch (Exception e) {
            DbLog.error(BusinessModuleEnum.BILIBILI,"获取b站设备标识异常", e);
            return null;
        }
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
                .timeout(REQUEST_TIMEOUT_MILLIS);
        for (Map.Entry<String, String> entry : getHeaders(false).entrySet()) {
            httpRequest.header(entry.getKey(), entry.getValue(), true);
        }
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
            Configs.save(ConfigKey.BILIBILI_COOKIES_TICKET, ticketBaseResp.getRaw());
            return ticketBaseResp.getData();
        } catch (Exception e) {
            return null;
        }
    }
    public BilibiliTickResp getTickInDb(){
        String s = Configs.getStr(ConfigKey.BILIBILI_COOKIES_TICKET, null);
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

        HttpRequest request = HttpRequest.post(s);
        for (Map.Entry<String, String> entry : getHeaders(false).entrySet()) {
            request.header(entry.getKey(), entry.getValue(), true);
        }
        try (HttpResponse response = request.execute()){
            String body = response.body();
            BilibiliBaseResp<BilibiliTickResp> tickResp = JSONObject.parseObject(body, new TypeReference<BilibiliBaseResp<BilibiliTickResp>>() {
            });
            tickResp.setRaw(body);
            if (tickResp.isSuccess()) {
                DbLog.info(BusinessModuleEnum.BILIBILI,"生成b站ticket url:{} resp:{}",s,body);
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
//            System.out.println(BilibiliSidUtil.generate());
//            Map<Long, LiveStatusInfo> liveStatusInfoByUids = bilibiliService.getLiveStatusInfoByUids(List.of(63231L, 416376577L));
//            System.out.println(liveStatusInfoByUids);


            BilibiliBaseResp<VideoDetail> videoDetail = bilibiliService.getVideoDetail("BV1ZsaB6HE2U");
            System.out.println(videoDetail.getRaw());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * 发起一次b站GET请求
     * <p>
     * url 中包含 {@code /wbi/} 的接口会自动做WBI签名（wts+w_rid）；
     * 签名后仍返回 -403（mixin key 过期）时刷新一次key再重试。
     */
    public <T> BilibiliBaseResp<T> sendGetRequest(String url, HashMap<String, Object> urlParam, TypeReference<BilibiliBaseResp<T>> responseType){
        boolean wbiApi = url.contains("/wbi/");
        BilibiliBaseResp<T> resp = doGet(url, urlParam, responseType, wbiApi, false);
        if (wbiApi && Objects.equals(CODE_WBI_INVALID, codeOf(resp))) {
            DbLog.warn(BusinessModuleEnum.BILIBILI,"b站wbi签名校验失败(-403)，刷新mixin key后重试 url:{}", url);
            resp = doGet(url, urlParam, responseType, true, true);
        }
        return resp;
    }

    private <T> BilibiliBaseResp<T> doGet(String url, HashMap<String, Object> urlParam, TypeReference<BilibiliBaseResp<T>> responseType,
                                         boolean wbiSign, boolean refreshWbiKey){
        String s = buildUrl(url, urlParam, wbiSign, refreshWbiKey);
        Map<String, String> headers = getHeaders(true);
        try (HttpResponse execute = createGetRequest(s, true).execute()){
            int status = execute.getStatus();
            String body = execute.body();
            BilibiliBaseResp<T> resp = JSONObject.parseObject(body, responseType);
            if (Objects.isNull(resp)) {
                // 412等被拦截的响应不是业务json，这里要兜住，避免上层空指针
                resp = new BilibiliBaseResp<>();
                resp.setCode(status);
                resp.setMessage("响应解析失败");
                resp.setTtl(1L);
            }
            resp.setRaw(body);
            if (isRiskBlocked(resp) || status == HTTP_STATUS_PRECONDITION_FAILED) {
                markRiskBlocked();
                DbLog.error(BusinessModuleEnum.BILIBILI,"b站接口被风控 url:{} status:{} headers:{} body:{}", s, status, maskHeaders(headers), body);
            } else if (!resp.isSuccess()) {
                DbLog.error(BusinessModuleEnum.BILIBILI,"b站接口响应异常 url:{} status:{} headers:{} body:{}", s, status, maskHeaders(headers), body);
            } else {
                DbLog.info(BusinessModuleEnum.BILIBILI,"b站接口响应 url:{} status:{} headers:{} body:{}", s, status, maskHeaders(headers), body);
            }
            return resp;
        } catch (Exception e) {
            DbLog.error(BusinessModuleEnum.BILIBILI,"请求b站接口异常 url:{}", s, e);
            return null;
        }
    }

    /**
     * 构造GET请求
     * <p>
     * 注意必须用 {@code header(name, value, true)} 覆盖hutool自带的默认请求头：
     * hutool 的 {@code HttpRequest.get()} 自带一个 "Chrome/75 ... Hutool" 的UserAgent和默认Accept，
     * 用 {@code addHeaders} 是追加，实际会发出两个User-Agent和两个Accept，这本身就是一个很好认的爬虫特征
     */
    private HttpRequest createGetRequest(String url, boolean cookie){
        HttpRequest httpRequest = HttpRequest.get(url).timeout(REQUEST_TIMEOUT_MILLIS);
        for (Map.Entry<String, String> entry : getHeaders(cookie).entrySet()) {
            httpRequest.header(entry.getKey(), entry.getValue(), true);
        }
        return httpRequest;
    }

    /**
     * 日志里隐藏Cookie等敏感请求头，避免SESSDATA被打进数据库
     */
    private String maskHeaders(Map<String, String> headers){
        Map<String, String> masked = new LinkedHashMap<>(headers.size());
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            String name = entry.getKey();
            String value = entry.getValue();
            boolean sensitive = "cookie".equalsIgnoreCase(name) || "authorization".equalsIgnoreCase(name);
            masked.put(name, sensitive && value != null ? "<hidden len=" + value.length() + ">" : value);
        }
        return JSONObject.toJSONString(masked);
    }

    private Integer codeOf(BilibiliBaseResp<?> resp){
        return Objects.isNull(resp) ? null : resp.getCode();
    }

    private boolean isRiskBlocked(BilibiliBaseResp<?> resp){
        return Objects.equals(CODE_RISK_BLOCKED, codeOf(resp));
    }

    /**
     * 记录一次风控，冷却期内不再请求b站接口
     */
    private void markRiskBlocked(){
        long until = System.currentTimeMillis() + RISK_BLOCK_COOL_DOWN_MILLIS;
        riskBlockedUntil = until;
        DbLog.warn(BusinessModuleEnum.BILIBILI,"b站接口被风控，进入{}秒冷却，期间不再请求b站接口", RISK_BLOCK_COOL_DOWN_MILLIS / 1000);
    }

    private boolean inRiskCoolDown(){
        long until = riskBlockedUntil;
        if (until <= 0) {
            return false;
        }
        if (System.currentTimeMillis() >= until) {
            riskBlockedUntil = 0;
            return false;
        }
        return true;
    }

    /**
     * 构造一个"请求被拦截"的响应，让调用方走失败分支，同时避免空指针
     */
    private <T> BilibiliBaseResp<T> blockedResp(String message){
        BilibiliBaseResp<T> resp = new BilibiliBaseResp<>();
        resp.setCode(CODE_RISK_BLOCKED);
        resp.setMessage(message);
        resp.setTtl(1L);
        resp.setRaw("");
        return resp;
    }

    private <T> BilibiliBaseResp<T> getCache(Map<String, CacheEntry<BilibiliBaseResp<T>>> cache, String key){
        if (StringUtils.isBlank(key)) {
            return null;
        }
        CacheEntry<BilibiliBaseResp<T>> entry = cache.get(key);
        if (Objects.isNull(entry)) {
            return null;
        }
        if (entry.expired()) {
            cache.remove(key);
            return null;
        }
        return entry.value;
    }

    private <T> void putCache(Map<String, CacheEntry<BilibiliBaseResp<T>>> cache, String key, BilibiliBaseResp<T> value, long ttlMillis){
        if (StringUtils.isBlank(key) || Objects.isNull(value)) {
            return;
        }
        if (cache.size() >= CACHE_MAX_SIZE) {
            cache.clear();
        }
        cache.put(key, new CacheEntry<>(value, System.currentTimeMillis() + ttlMillis));
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
        try (HttpResponse response = createGetRequest(BILIBILI_NAV_URL, true).execute()) {
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
        downloadVideo(url, file, timeout, null);
    }

    /**
     * 下载视频，progress 不为空时回调下载进度（hutool 会带上content-length与已下载字节数）
     */
    public void downloadVideo(String url, File file, int timeout, StreamProgress progress){
        HttpRequest httpRequest = HttpUtil.createGet(url, true)
                .addHeaders(getHeaders(false))
                .timeout(timeout);
        try (HttpResponse response = httpRequest.execute()){
            response.writeBody(file, progress);
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

    /**
     * 带过期时间的缓存项
     */
    private static final class CacheEntry<T> {
        private final T value;
        private final long expireAt;

        private CacheEntry(T value, long expireAt) {
            this.value = value;
            this.expireAt = expireAt;
        }

        private boolean expired() {
            return System.currentTimeMillis() > expireAt;
        }
    }
}
