package com.haruhi.botServer.service;

import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSONObject;
import com.haruhi.botServer.dto.jmcomic.*;
import com.haruhi.botServer.utils.FileUtil;
import com.haruhi.botServer.utils.RestUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class JmcomicService {
    private static final String API_DOMAIN = "www.cdnblackmyth.club";
    private static final String APP_TOKEN_SECRET = "18comicAPP";
    private static final String APP_TOKEN_SECRET_2 = "18comicAPPContent";
    private static final String APP_DATA_SECRET = "185Hcomic3PAPP7R";
    private static final String APP_VERSION = "1.7.5";
    private static final String IMAGE_DOMAIN = "cdn-msp2.jmapiproxy2.cc";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";


    public UserProfile login(String username, String password) throws Exception {
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
        String data = decryptData(ts, jsonObject.getString("data"));
        return JSONObject.parseObject(data, UserProfile.class);
    }


    public void downAlbum(String aid) throws Exception {
        Album album = album(aid);
        if (CollectionUtils.isEmpty(album.getSeries())) {
            Series series = new Series();
            series.setSort("1");
            series.setTitle("第1话");
            series.setId(aid);
            album.setSeries(Collections.singletonList(series));
        }
        String albumName = album.getName() + "_" + aid;
        String albumPath = FileUtil.getJmcomicDir() + File.separator + albumName;
        for (Series series : album.getSeries()) {
            series.setTitle("第" + series.getSort() +"话");
            try {
                String chapterPath = albumPath + File.separator + (series.getTitle()+"_" + (StringUtils.isBlank(series.getName()) ? "" : series.getName()));
                Chapter chapter = chapter(series.getId());
                downChapter(chapter,chapterPath,10 * 1000);
            }catch (Exception e) {
                log.error("下载章节异常 a:{} c:{}",JSONObject.toJSONString(album), JSONObject.toJSONString(series));
            }
        }
    }


    public void downChapter(Chapter chapter,String chapterPath,int timeout) throws Exception {
        List<String> images = chapter.getImages();
        if (CollectionUtils.isEmpty(images)) {
            log.error("该章节无图片 c:{}",JSONObject.toJSONString(chapter));
            return;
        }
        List<DownloadParam> collect = images.stream().map(filename -> {
            DownloadParam downloadParam = new DownloadParam();

            downloadParam.setImgUrl(buildImgUrl(chapter.getId(), filename));
            downloadParam.setFilename(filename);
            downloadParam.setImgFilePath(chapterPath + File.separator + filename);

            return downloadParam;
        }).collect(Collectors.toList());

        RestTemplate restTemplate = new RestTemplate();
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeout);
        requestFactory.setReadTimeout(timeout);
        restTemplate.setRequestFactory(requestFactory);


        collect.forEach(param -> {
            try {
                File file = new File(param.getImgFilePath());

//                ResponseEntity<byte[]> responseEntity = RestUtil.sendGetRequest(restTemplate, param, null, null,
//                        new ParameterizedTypeReference<byte[]>() {});
//                byte[] body = responseEntity.getBody();
//                FileUtils.writeByteArrayToFile(file, body);

//                ResponseEntity<InputStream> responseEntity = RestUtil.sendGetRequest(restTemplate, param.getImgUrl(), null, null,
//                        new ParameterizedTypeReference<InputStream>() {});
//                InputStream body = responseEntity.getBody();
//                FileUtils.copyInputStreamToFile(body,file);

                System.out.println(param.getImgUrl());
                byte[] bytes = HttpUtil.downloadBytes(param.getImgUrl());
//                FileUtils.writeByteArrayToFile(file, bytes);
                saveImg(8,bytes,file);
//                FileUtils.writeByteArrayToFile(file, bytes);
            }catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void saveImg(int blockNum,byte[] bytes,File file) throws Exception {
        BufferedImage srcImg = ImageIO.read(new ByteArrayInputStream(bytes));
        // 2. 转换图像格式（保持与Rust一致的RGB8）
        BufferedImage rgbImg = new BufferedImage(
                srcImg.getWidth(),
                srcImg.getHeight(),
                BufferedImage.TYPE_INT_RGB
        );
        rgbImg.createGraphics().drawImage(srcImg, 0, 0, null);
        BufferedImage dstImg = blockNum == 0 ? rgbImg : stitchImg(rgbImg, blockNum);
        ImageIO.write(dstImg, "webp", file);
//        FileUtils.writeByteArrayToFile(file, bytes);
    }

    private BufferedImage stitchImg(BufferedImage srcImg, int blockNum) {
        int width = srcImg.getWidth();
        int height = srcImg.getHeight();
        BufferedImage stitchedImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        int remainderHeight = height % blockNum;
        WritableRaster dstRaster = stitchedImg.getRaster();
        Raster srcRaster = srcImg.getRaster();

        for (int i = 0; i < blockNum; i++) {
            int blockHeight = height / blockNum;
            if (i == 0 && remainderHeight > 0) {
                blockHeight += remainderHeight;
            }

            int srcStartY = height - blockHeight * (i + 1);
            int dstStartY = blockHeight * i + (i == 0 ? 0 : remainderHeight);

            // 逐行复制像素
            for (int y = 0; y < blockHeight; y++) {
                int[] srcPixels = new int[width];
                srcRaster.getDataElements(0, srcStartY + y, width, 1, srcPixels);
                dstRaster.setDataElements(0, dstStartY + y, width, 1, srcPixels);
            }
        }
        return stitchedImg;
    }

    public Album album(String aid) throws Exception {
        String url = "https://" + API_DOMAIN + "/album";
        long ts = System.currentTimeMillis() / 1000;
        HttpHeaders headerParam = headerParam(ts);

        HashMap<String, Object> urlParam = new HashMap<>();
        urlParam.put("id", aid);
        ResponseEntity<String> responseEntity = RestUtil.sendGetRequest(RestUtil.getRestTemplate(5000),
                url,  urlParam, headerParam, new ParameterizedTypeReference<String>() {
                });
        JSONObject jsonObject = JSONObject.parseObject(responseEntity.getBody());
        String data = decryptData(ts, jsonObject.getString("data"));
        return JSONObject.parseObject(data,Album.class);
    }


    public Chapter chapter(String chapterId)throws Exception{
        String url = "https://" + API_DOMAIN + "/chapter";
        long ts = System.currentTimeMillis() / 1000;
        HttpHeaders headerParam = headerParam(ts);

        HashMap<String, Object> urlParam = new HashMap<>();
        urlParam.put("id", chapterId);
        ResponseEntity<String> responseEntity = RestUtil.sendGetRequest(RestUtil.getRestTemplate(5000),
                url,  urlParam, headerParam, new ParameterizedTypeReference<String>() {
                });
        JSONObject jsonObject = JSONObject.parseObject(responseEntity.getBody());
        String data = decryptData(ts, jsonObject.getString("data"));
        return JSONObject.parseObject(data, Chapter.class);
    }



    private String buildImgUrl(Long chapterId,String filename) {
        return "https://" + IMAGE_DOMAIN + "/media/photos/"+ chapterId +"/"+ filename;
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

        return new String(decryptedData, StandardCharsets.UTF_8);
    }


    public static void main(String[] args) {
        try {
            JmcomicService jmcomicService = new JmcomicService();
//            UserProfile login = jmcomicService.login("","");
//            System.out.println("login = " + login);
//            Album album = jmcomicService.album("287058");
//            System.out.println("album = " + album);
//            Chapter chapter = jmcomicService.chapter("287058");
//            System.out.println("chapter = " + chapter);

            jmcomicService.downAlbum("517158");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
