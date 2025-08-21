package com.haruhi.botServer.service.music.extend;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.haruhi.botServer.constant.ThirdPartyURL;
import com.haruhi.botServer.dto.music163.Song;
import com.haruhi.botServer.dto.music163.SearchResp;
import com.haruhi.botServer.factory.MusicServiceFactory;
import com.haruhi.botServer.service.music.AbstractMusicService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * 网易点歌service类
 */
@Slf4j
@Service
public class Music163Service extends AbstractMusicService {

    @Override
    public List<Song> searchMusic(Object param) {
        String res = search(String.valueOf(param));
        if (StringUtils.isBlank(res)) {
            return null;
        }
        List<SearchResp> searchResps = parseJavaBean(res);
        if(CollectionUtils.isEmpty(searchResps)){
            return null;
        }
        List<Song> result = new ArrayList<>(searchResps.size());
        for (SearchResp searchResp : searchResps) {
            Song song = new Song();
            // set 歌曲id
            song.setId(searchResp.getId());
            // set歌曲名称
            String name = searchResp.getName();
            List<String> tns = searchResp.getTns();
            if (!CollectionUtils.isEmpty(tns)) {
                // 后缀加上翻译名
                name += MessageFormat.format(" - ({0})",tns.get(0));
            }
            song.setName(name);
            // set专辑名称
            SearchResp.AL al = searchResp.getAl();
            song.setAlbumName(al != null ? al.getName() : null);
            // set歌手名称
            List<SearchResp.AR> ar = searchResp.getAr();
            String artists = "";
            if (!CollectionUtils.isEmpty(ar)) {
                int size = ar.size();
                if(size == 1){
                    artists = ar.get(0).getName();
                }else{
                    StringBuilder stringBuilder = new StringBuilder();
                    for (int i = 0; i < size; i++) {
                        stringBuilder.append(ar.get(i).getName());
                        if(size > i + 1){
                            stringBuilder.append("/");
                        }
                    }
                    artists = stringBuilder.toString();
                }
            }
            song.setArtists(artists);
            result.add(song);
        }
        return result;
    }

    private List<SearchResp> parseJavaBean(String s){
        try {
            JSONObject resJsonObj = JSONObject.parseObject(s);

            JSONObject resultJson = resJsonObj.getJSONObject("result");
            if(resultJson == null){
                return null;
            }
            String result = resultJson.getString("songs");

            List<SearchResp> searchResps = JSONArray.parseArray(result, SearchResp.class);
            return searchResps;
        }catch (Exception e){
            log.error("网易搜索结果(json)转SearchResp对象异常",e);
            return null;
        }
    }

    @Override
    public MusicServiceFactory.MusicType type() {
        return MusicServiceFactory.MusicType.cloudMusic;
    }

    /**
     * 请求网易接口，返回原json串
     * @param keyWord 歌曲名
     * @return
     */
    public String search(String keyWord) {
        Map<String, Object> map = new HashMap<>(6);
        map.put("s", keyWord);
        map.put("type", 1);
        map.put("limit", 20);
        map.put("total", "true");
        map.put("offset", 0);
        map.put("csrf_token", "");
        log.info("开始搜索歌曲(163)：{}",keyWord);
        long l = System.currentTimeMillis();
        String s = HttpUtil.urlWithForm(ThirdPartyURL.NETEASE_SEARCH_MUSIC, prepare(map), StandardCharsets.UTF_8, true);
        HttpRequest httpRequest = HttpUtil.createPost(encode(s)).timeout(10000);
        try (HttpResponse response = httpRequest.execute()){
            log.info("搜索完成(163)，耗时：{}",System.currentTimeMillis() - l);
            return response.body();
        }
    }
    private String encode(String url) {
        return url.replace("\\","%5C")
                .replace("+","%2B")
                .replace(" ","%20")
//                .replace("%","%25")
                .replace("#","%23")
                .replace("$","%24")
                .replace("^","%5E")
                .replace("{","%7B")
                .replace("}","%7D")
                .replace("|","%7C")
                .replace("[","%5B")
                .replace("]","%5D");

    }

    private Map<String, Object> prepare(Map<String, Object> raw) {
        Map<String, Object> data = new HashMap<>(2);
        String NONCE = "0CoJUm6Qyw8W8jud";
        String params = encrypt(JSONObject.toJSONString(raw), NONCE);
        String secretKey = "TA3YiYCfY2dDJQgg";
        data.put("params", encrypt(params, secretKey));
        String encSecKey = "84ca47bca10bad09a6b04c5c927ef077d9b9f1e37098aa3eac6ea70eb59df0aa28b691b7e75e4f1f9831754919ea784c8f74fbfadf2898b0be17849fd656060162857830e241aba44991601f137624094c114ea8d17bce815b0cd4e5b8e2fbaba978c6d1d14dc3d1faf852bdd28818031ccdaaa13a6018e1024e2aae98844210";
        data.put("encSecKey", encSecKey);
        return data;
    }

    private String encrypt(String content, String password) {
        try {
            SecretKeySpec key = new SecretKeySpec(password.getBytes(), "AES");
            // 创建密码器
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            String VI = "0102030405060708";
            // 创建iv
            IvParameterSpec iv = new IvParameterSpec(VI.getBytes());
            byte[] byteContent = content.getBytes(StandardCharsets.UTF_8);
            // 初始化
            cipher.init(Cipher.ENCRYPT_MODE, key, iv);
            byte[] result = cipher.doFinal(byteContent);
            // 加密
            return Base64.getEncoder().encodeToString(result);
        } catch (Exception e) {
            log.error("搜索歌曲加密时异常",e);
            return null;
        }
    }

}
