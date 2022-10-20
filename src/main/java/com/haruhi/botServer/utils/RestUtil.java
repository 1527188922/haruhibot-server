package com.haruhi.botServer.utils;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class RestUtil {
    private RestUtil(){}

    public static <T> T sendGetRequest(RestTemplate restTemplate, String url,Map<String, Object> urlRequestParam, Class<T> type){
        return RestUtil.sendRequest(restTemplate,url, HttpMethod.GET,null,urlRequestParam,type);
    }
    public static <T,O> T sendPostRequest(RestTemplate restTemplate, String url,O msgBody, Map<String, Object> urlRequestParam, Class<T> type){
        return RestUtil.sendRequest(restTemplate,url,HttpMethod.POST,msgBody,urlRequestParam,type);
    }
    private static <T,O> T sendRequest(RestTemplate restTemplate, String url,HttpMethod method ,O msgBody, Map<String, Object> urlRequestParam, Class<T> type){
        try {
            // 设置请求头
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.setContentType(MediaType.APPLICATION_JSON);
            httpHeaders.add("Accept", MediaType.APPLICATION_JSON_VALUE);
            HttpEntity<O> entity = new HttpEntity<>(msgBody, httpHeaders);
            ResponseEntity<String> response = null;
            log.info("发起rest请求：{}，payload:{}，urlParam:{}",url,JSONObject.toJSONString(msgBody),JSONObject.toJSONString(urlRequestParam));
            if(urlRequestParam != null){
                response = restTemplate.exchange(urlSplicing(url,urlRequestParam), method, entity, new ParameterizedTypeReference<String>() {
                });
            }else{
                response = restTemplate.exchange(url, method,entity,new ParameterizedTypeReference<String>() {
                });
            }

            return processResponse(response,type);
        }catch (Exception e){
            log.error("rest请求发生异常,url:{}",url,e);
            return null;
        }
    }

    public static String urlSplicing(String url,Map<String,Object> param){
        StringBuilder sb=new StringBuilder("?");
        for(Map.Entry<String,Object> map:param.entrySet()){
            sb.append(map.getKey()+"="+(map.getValue())+"&");
        }
        return url.concat(sb.substring(0, sb.length() - 1));
    }

    /**
     * form提交
     * 可发送文件
     * @param restTemplate
     * @param url
     * @param param
     * @param type
     * @param <T>
     * @return
     */
    public static <T> T sendPostForm(RestTemplate restTemplate, String url, LinkedMultiValueMap<String,Object> param,Class<T> type){
        ResponseEntity<String> response = restTemplate.postForEntity(url, param, String.class, (Object) null);
        return processResponse(response,type);
    }

    private static <T> T processResponse(ResponseEntity<String> response,Class<T> tClass){
        if(response == null ){
            log.info("http请求响应结果为空 ResponseEntity == null");
            return null;
        }
        if(response.getStatusCodeValue() != 200){
            log.info("http请求响应状态码异常:{}\n{}",response.getStatusCode().value(),response);
            return null;
        }
        String body = response.getBody();
        if(body == null){
            log.info("接口响应结果为null");
            return null;
        }

        if(tClass == String.class){
            return (T)body;
        }
        try {
            return JSONObject.parseObject(body, tClass);
        }catch (Exception e){
            log.error("请求结果(json串)转javabean异常",e);
            return null;
        }
    }
    private static RestTemplate restTemplate = new RestTemplate();

    public static RestTemplate getRestTemplate(){
        return restTemplate;
    }

    /**
     * 存放RestTemplate对象
     * key:timeout
     */
    private static Map<Integer,RestTemplate> restTemplateCache = new ConcurrentHashMap<>();
    /**
     * 获取指定超时时间的RestTemplate
     * @param timeout
     * @return
     */
    public static RestTemplate getRestTemplate(int timeout) {
        RestTemplate restTemp = restTemplateCache.get(timeout);
        if(restTemp != null){
            return restTemp;
        }

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeout);
        requestFactory.setReadTimeout(timeout);
        restTemp = new RestTemplate();
        restTemp.setRequestFactory(requestFactory);
        restTemp.getMessageConverters().set(1, new StringHttpMessageConverter(StandardCharsets.UTF_8));

        restTemplateCache.put(timeout,restTemp);
        return restTemp;
    }
}
