package com.haruhi.botServer.picimagesearch.engine;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSONObject;
import com.haruhi.botServer.picimagesearch.HttpData;
import com.haruhi.botServer.picimagesearch.PicImageSearchException;
import com.haruhi.botServer.picimagesearch.PicImageSearchUtil;
import org.apache.commons.lang3.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;


public abstract class AbstractSearchEngine implements SearchEngine {
        private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                + "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/99.0.4844.82 Safari/537.36";

        protected final String baseUrl;
        protected int timeoutMillis = 30_000;
        protected Map<String, String> headers = new LinkedHashMap<>();
        protected String cookies;

        protected AbstractSearchEngine(String baseUrl) {
            this.baseUrl = PicImageSearchUtil.stripTrailingSlash(baseUrl);
            this.headers.put("User-Agent", USER_AGENT);
        }

        public AbstractSearchEngine timeoutMillis(int timeoutMillis) {
            this.timeoutMillis = timeoutMillis;
            return this;
        }

        public AbstractSearchEngine header(String name, String value) {
            this.headers.put(name, value);
            return this;
        }

        public AbstractSearchEngine cookies(String cookies) {
            this.cookies = cookies;
            return this;
        }

        protected HttpData get(String endpoint, Map<String, ?> params) {
            return request("GET", endpoint, params, null, null, null);
        }

        protected HttpData postForm(String endpoint, Map<String, ?> params, Map<String, ?> form, Map<String, ?> files) {
            return request("POST", endpoint, params, form, files, null);
        }

        protected HttpData postJson(String endpoint, Map<String, ?> params, Object json) {
            return request("POST", endpoint, params, null, null, JSONObject.toJSONString(json));
        }

        protected byte[] download(String url) {
            try (HttpResponse response = baseRequest(HttpRequest.get(url)).execute()) {
                if (!response.isOk()) {
                    throw new PicImageSearchException("download failed: " + response.getStatus());
                }
                return response.bodyBytes();
            }
        }

        private HttpData request(String method, String endpoint, Map<String, ?> params, Map<String, ?> form,
                                 Map<String, ?> files, String body) {
            String url = buildUrl(endpoint);
            if (params != null && !params.isEmpty()) {
                url = HttpUtil.urlWithForm(url, PicImageSearchUtil.toObjectMap(params), StandardCharsets.UTF_8, false);
            }
            HttpRequest request = "POST".equalsIgnoreCase(method) ? HttpRequest.post(url) : HttpRequest.get(url);
            baseRequest(request);
            if (form != null) {
                form.forEach(request::form);
            }
            if (files != null) {
                files.forEach(request::form);
            }
            if (body != null) {
                request.body(body, "application/json;charset=UTF-8");
            }
            try (HttpResponse response = request.execute()) {
                return new HttpData(response.body(), response.getStatus(), url);
            }
        }

        private HttpRequest baseRequest(HttpRequest request) {
            request.timeout(timeoutMillis).headerMap(headers, true);
            if (StringUtils.isNotBlank(cookies)) {
                request.cookie(cookies);
            }
            return request;
        }

        protected String buildUrl(String endpoint) {
            if (StringUtils.isBlank(endpoint)) {
                return baseUrl;
            }
            if (endpoint.startsWith("http://") || endpoint.startsWith("https://")) {
                return endpoint;
            }
            return baseUrl + "/" + PicImageSearchUtil.trimSlash(endpoint);
        }
    }
