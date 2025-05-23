package com.haruhi.botServer.utils;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class RestUtil {
    private RestUtil(){}

    public static <T> T sendGetRequest(RestTemplate restTemplate, String url,Map<String, Object> urlRequestParam, Class<T> type){
        return RestUtil.sendRequest(restTemplate,url, HttpMethod.GET,null,urlRequestParam,type);
    }
    public static <T> T sendPostRequest(RestTemplate restTemplate, String url, Object reqBody, Map<String, Object> urlParam,
                                        HttpHeaders headers, ParameterizedTypeReference<T> respType){
        if(headers == null){
            headers = new HttpHeaders();
        }
        if(headers.getContentType() == null){
            headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
        }
        HttpEntity<Object> httpEntity = new HttpEntity<>(reqBody,headers);
        ResponseEntity<T> responseEntity = restTemplate.exchange(urlSplicing(url,urlParam), HttpMethod.POST, httpEntity, respType);
        return responseEntity.getBody();
    }

    public static <T> ResponseEntity<T> sendGetRequest(RestTemplate restTemplate, String url, Map<String, Object> urlParam,
                                        HttpHeaders headers, ParameterizedTypeReference<T> respType){
        if(headers == null){
            headers = new HttpHeaders();
        }
        if(headers.getContentType() == null){
            headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
        }
        HttpEntity<Object> httpEntity = new HttpEntity<>(headers);
        ResponseEntity<T> responseEntity = restTemplate.exchange(urlSplicing(url,urlParam), HttpMethod.GET, httpEntity, respType);
        return responseEntity;
    }

    private static <T,O> T sendRequest(RestTemplate restTemplate, String url,HttpMethod method ,O msgBody, Map<String, Object> urlRequestParam, Class<T> type){
        try {
            // 设置请求头
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.setContentType(MediaType.APPLICATION_JSON);
            httpHeaders.add("Accept", MediaType.APPLICATION_JSON_VALUE);
            HttpEntity<O> entity = new HttpEntity<>(msgBody, httpHeaders);
            ResponseEntity<String> response = restTemplate.exchange(urlSplicing(url,urlRequestParam), method, entity, new ParameterizedTypeReference<String>() {
            });
            log.info("发起rest请求：{}，payload:{}，urlParam:{}",url,JSONObject.toJSONString(msgBody),JSONObject.toJSONString(urlRequestParam));
            return processResponse(response,type);
        }catch (Exception e){
            log.error("rest请求发生异常,url:{}",url,e);
            return null;
        }
    }

    public static String urlSplicing(String url,Map<String,Object> param){
        if(CollectionUtils.isEmpty(param)){
            return url;
        }
        StringBuilder sb=new StringBuilder("?");
        for(Map.Entry<String,Object> map:param.entrySet()){
            sb.append(map.getKey()+"="+(map.getValue())+"&");
        }
        return url.concat(sb.substring(0, sb.length() - 1));
    }

    /**
     * 表单请求
     * @param restTemplate
     * @param url
     * @param formData
     * @param urlRequestParam
     * @param headerParam
     * @param responseType
     * @param <T>
     * @return
     */
    public static <T> ResponseEntity<T> sendPostForm(RestTemplate restTemplate, String url, LinkedMultiValueMap<String, Object> formData,  Map<String, String> urlRequestParam, Map<String, String> headerParam
    ,ParameterizedTypeReference<T> responseType){

        if(urlRequestParam != null){
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
            for(Map.Entry<String,String> e:urlRequestParam.entrySet()){
                builder.queryParam(e.getKey(),e.getValue());
            }
            url = builder.toUriString();
        }
        
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);
        if(headerParam != null){
            for(Map.Entry<String,String> e : headerParam.entrySet()){
                httpHeaders.add(e.getKey(),e.getValue());
            }
        }
        HttpEntity<LinkedMultiValueMap<String, Object>> httpEntity = new HttpEntity<>(formData, httpHeaders);
        return restTemplate.exchange(url, HttpMethod.POST,httpEntity, responseType);
    }

    public static void main(String[] args) {
        LinkedMultiValueMap<String,Object> param = new LinkedMultiValueMap<>(6);
        param.add("output_type",2);
        param.add("api_key", "88b9b924fb764eb9ebe70db30bbd5da4a184b5a4");
        param.add("testmode",1);
        param.add("numres",6);
        param.add("db",99);
        param.add("file",new FileSystemResource(new File("D:\\temp\\8eb6-kefmphc8903849.jpg")));
//        param.add("file",new FileSystemResource(new File("")));
        ResponseEntity<String> stringResponseEntity = sendPostForm(getRestTemplate(10000), "https://saucenao.com/search.php", param,
                null, null, new ParameterizedTypeReference<String>() {
                });

        System.out.println(stringResponseEntity);
        
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
        log.info("http请求响应：{}",body);
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
