package com.haruhi.botServer.picimagesearch.engine;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.haruhi.botServer.picimagesearch.*;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;


@Component
public class AnimeTrace extends AbstractSearchEngine {
        private Integer isMulti;
        private Integer aiDetect;

        public AnimeTrace() {
            super("https://api.animetrace.com/v1/search");
        }

        public AnimeTrace isMulti(Integer isMulti) {
            this.isMulti = isMulti;
            return this;
        }

        public AnimeTrace aiDetect(Integer aiDetect) {
            this.aiDetect = aiDetect;
            return this;
        }

        @Override
        public EngineType type() {
            return EngineType.ANIME_TRACE;
        }

        @Override
        public SearchResponse search(SearchInput input) {
            Map<String, Object> params = new LinkedHashMap<>();
            if (isMulti != null) {
                params.put("is_multi", isMulti);
            }
            if (aiDetect != null) {
                params.put("ai_detect", aiDetect);
            }
            HttpData http;
            if (input.hasUrl() || input.hasBase64()) {
                Map<String, Object> jsonBody = new LinkedHashMap<>(params);
                input.url().ifPresent(url -> jsonBody.put("url", url));
                input.base64().ifPresent(base64 -> jsonBody.put("base64", base64));
                http = postJson("", null, jsonBody);
            } else if (input.hasFile()) {
                http = postForm("", null, params, Map.of("file", input.file().orElseThrow()));
            } else {
                throw new IllegalArgumentException("One of url, file or base64 must be provided");
            }
            JSONObject json = PicImageSearchUtil.parseJson(http.body());
            SearchResponse response = new SearchResponse(type(), http.url(), json, http.statusCode())
                    .put("code", json.get("code"))
                    .put("ai", json.get("ai"))
                    .put("trace_id", json.get("trace_id"));
            JSONArray data = json.getJSONArray("data");
            if (data != null) {
                for (Object obj : data) {
                    JSONObject item = (JSONObject) obj;
                    SearchItem searchItem = new SearchItem()
                            .origin(item)
                            .put("box", item.get("box"))
                            .put("box_id", item.get("box_id"))
                            .put("characters", item.get("character"));
                    JSONArray characters = item.getJSONArray("character");
                    if (characters != null && !characters.isEmpty()) {
                        JSONObject first = characters.getJSONObject(0);
                        searchItem.title(first.getString("character")).source(first.getString("work"));
                    }
                    response.add(searchItem);
                }
            }
            return response;
        }
    }
