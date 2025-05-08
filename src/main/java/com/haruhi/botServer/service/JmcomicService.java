package com.haruhi.botServer.service;

import com.alibaba.fastjson.JSONObject;
import com.haruhi.botServer.utils.RestUtil;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

@Service
public class JmcomicService {
    private static final String API_DOMAIN = "www.cdnblackmyth.club";
    private static final String APP_TOKEN_SECRET = "18comicAPP";
    private static final String APP_TOKEN_SECRET_2 = "18comicAPPContent";
    private static final String APP_DATA_SECRET = "185Hcomic3PAPP7R";
    private static final String APP_VERSION = "1.7.5";
    private static final String IMAGE_DOMAIN = "cdn-msp2.jmapiproxy2.cc";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";


    public String login(String username, String password) throws Exception {
        String url = "https://" + API_DOMAIN + "/login";
        LinkedMultiValueMap<String, Object> formData = new LinkedMultiValueMap<>();
        formData.add("username", username);
        formData.add("password", password);

        long ts = System.currentTimeMillis() / 1000;
        HttpHeaders headerParam = headerParam(ts);
        ResponseEntity<String> responseEntity = RestUtil.sendPostForm(RestUtil.getRestTemplate(5000), url, formData, null,
                headerParam.toSingleValueMap(), new ParameterizedTypeReference<String>() {
        });
        JSONObject jsonObject = JSONObject.parseObject(responseEntity.getBody());
        String data = jsonObject.getString("data");
        return decryptData(ts, data);
    }


    public String album(String aid) throws Exception {
        String url = "https://" + API_DOMAIN + "/album";
        long ts = System.currentTimeMillis() / 1000;
        HttpHeaders headerParam = headerParam(ts);

        HashMap<String, Object> urlParam = new HashMap<>();
        urlParam.put("id", aid);
        ResponseEntity<String> responseEntity = RestUtil.sendGetRequest(RestUtil.getRestTemplate(5000),
                url,  urlParam, headerParam, new ParameterizedTypeReference<String>() {
                });
        JSONObject jsonObject = JSONObject.parseObject(responseEntity.getBody());
        return decryptData(ts, jsonObject.getString("data"));
    }


    private HttpHeaders headerParam(long ts){
        HttpHeaders httpHeaders = new HttpHeaders();

        String token = DigestUtils.md5Hex(ts + APP_TOKEN_SECRET);
        String tokenParam = ts + "," + APP_VERSION;
        httpHeaders.add("token",token);
        httpHeaders.add("tokenparam",tokenParam);
        httpHeaders.add("user-agent",USER_AGENT);
        return httpHeaders;
    }

    private String decryptData(long ts, String encryptedData) throws Exception {
        byte[] encryptedBytes = Base64.decodeBase64(encryptedData);

        // 生成密钥
        String keyStr = ts + APP_DATA_SECRET;
        String hexStr = DigestUtils.md5Hex(keyStr); // 将16字节的MD5哈希转换为32字符的十六进制字符串
        byte[] key = hexStr.getBytes(StandardCharsets.US_ASCII); // 转换为32字节的密钥

        // 创建AES密钥
        SecretKeySpec secretKey = new SecretKeySpec(key, "AES");

        // 初始化Cipher进行解密，使用ECB模式和PKCS5Padding（对应PKCS7）
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);

        // 解密数据
        byte[] decryptedData = cipher.doFinal(encryptedBytes);

        // 去除PKCS7填充
//        int paddingLength = decryptedData[decryptedData.length - 1] & 0xFF;
//        byte[] decryptedWithoutPadding = new byte[decryptedData.length - paddingLength];
//        System.arraycopy(decryptedData, 0, decryptedWithoutPadding, 0, decryptedWithoutPadding.length);

        // 转换为UTF-8字符串
        return new String(decryptedData, StandardCharsets.UTF_8);
    }


    public static void main(String[] args) {
        try {
            JmcomicService jmcomicService = new JmcomicService();
            String login = jmcomicService.login("","");
            String album = jmcomicService.album("");
            System.out.println("login = " + login);
            System.out.println("album = " + album);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
