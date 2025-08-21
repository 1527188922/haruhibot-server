//package com.haruhi.botServer.utils;
//
//import cn.hutool.http.HttpUtil;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.http.HttpEntity;
//import org.apache.http.ParseException;
//import org.apache.http.client.config.RequestConfig;
//import org.apache.http.client.methods.CloseableHttpResponse;
//import org.apache.http.client.methods.HttpGet;
//import org.apache.http.client.methods.HttpPost;
//import org.apache.http.client.methods.HttpUriRequest;
//import org.apache.http.impl.client.CloseableHttpClient;
//import org.apache.http.impl.client.HttpClients;
//import org.apache.http.util.EntityUtils;
//
//import java.io.IOException;
//import java.nio.charset.StandardCharsets;
//import java.util.Map;
//
//@Slf4j
//public class HttpClientUtil {
//    public static CloseableHttpClient getHttpClient(int timeout){
//        RequestConfig requestConfig = RequestConfig.custom()
//                .setSocketTimeout(timeout)
//                .setConnectTimeout(timeout)
//                .setConnectionRequestTimeout(timeout)
//                .setStaleConnectionCheckEnabled(true)
//                .build();
//        return HttpClients.custom()
//                .setDefaultRequestConfig(requestConfig)
//                .build();
//    }
//
//    public static String doPost(String url, Map<String,Object> urlParams,int timeout){
//        String s = encode(getUrl(url, urlParams));
//        HttpPost httpPost = new HttpPost(s);
//        try {
//            return request(httpPost, timeout);
//        } catch (IOException e) {
//            log.error("HttpClient POST请求:{}异常",s,e);
//            return null;
//        }
//    }
//
//    public static String doGet(String url, Map<String,Object> urlParams,int timeout){
//        String s = encode(getUrl(url, urlParams));
//        HttpGet httpGet = new HttpGet(s);
//        try {
//            return request(httpGet,timeout);
//        } catch (Exception e) {
//            log.error("HttpClient GET请求:{}异常",s,e);
//            return null;
//        }
//    }
//
//    public static String doGetNoCatch(String url, Map<String,Object> urlParams,int timeout) throws IOException, ParseException{
//        String s = encode(getUrl(url, urlParams));
//        HttpGet httpGet = new HttpGet(s);
//
//        return request(httpGet,timeout);
//    }
//
//    private static String getUrl(String s,Map<String,Object> urlParams){
//        return HttpUtil.urlWithForm(s, urlParams, StandardCharsets.UTF_8,false);
//    }
//
//    private static String request(HttpUriRequest httpMethod,int timeout) throws IOException, ParseException {
//        try (CloseableHttpClient httpClient = getHttpClient(timeout);
//             CloseableHttpResponse response = httpClient.execute(httpMethod)){
//            HttpEntity entity = response.getEntity();
//            return EntityUtils.toString(entity,"UTF-8");
//        }
//    }
//
//    public static String encode(String url) {
//        return url.replace("\\","%5C")
//                .replace("+","%2B")
//                .replace(" ","%20")
////                .replace("%","%25")
//                .replace("#","%23")
//                .replace("$","%24")
//                .replace("^","%5E")
//                .replace("{","%7B")
//                .replace("}","%7D")
//                .replace("|","%7C")
//                .replace("[","%5B")
//                .replace("]","%5D");
//
//    }
//}
