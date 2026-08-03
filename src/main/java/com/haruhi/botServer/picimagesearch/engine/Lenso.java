package com.haruhi.botServer.picimagesearch.engine;

import cn.hutool.core.codec.Base64;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.haruhi.botServer.picimagesearch.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class Lenso extends AbstractSearchEngine {
        public Lenso() {
            super("https://lenso.ai");
        }

        @Override
        public EngineType type() {
            return EngineType.LENSO;
        }

        @Override
        public SearchResponse search(SearchInput input) {
            byte[] imageBytes;
            if (input.hasUrl()) {
                imageBytes = download(input.url().orElseThrow());
            } else if (input.hasFile()) {
                imageBytes = cn.hutool.core.io.FileUtil.readBytes(input.file().orElseThrow());
            } else {
                throw new IllegalArgumentException("Either url or file must be provided");
            }
            String imageBase64 = Base64.encode(imageBytes);
            HttpData upload = postJson("api/upload", null, Map.of("image", "data:image/jpeg;base64," + imageBase64));
            JSONObject uploadJson = PicImageSearchUtil.parseJson(upload.body());
            String id = uploadJson.getString("id");
            if (StringUtils.isBlank(id)) {
                throw new PicImageSearchException("Lenso upload failed: " + upload.body());
            }
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("image", Map.of("id", id, "data", "data:image/jpeg;base64," + imageBase64));
            payload.put("effects", Map.of());
            payload.put("selection", Map.of());
            payload.put("domain", "");
            payload.put("text", "");
            payload.put("page", 0);
            payload.put("type", "");
            payload.put("sort", "SMART");
            payload.put("seed", 0);
            payload.put("facial_search_consent", 0);
            HttpData search = postJson("api/search", null, payload);
            return parseLenso(PicImageSearchUtil.parseJson(search.body()), baseUrl + "/en/results/" + id, search.statusCode());
        }

        private SearchResponse parseLenso(JSONObject json, String url, int statusCode) {
            SearchResponse response = new SearchResponse(EngineType.LENSO, url, json, statusCode)
                    .put("detected_faces", json.get("detectedFaces"));
            JSONObject results = json.getJSONObject("results");
            if (results == null) {
                return response;
            }
            for (String type : List.of("duplicates", "similar", "places", "related", "people")) {
                JSONArray array = results.getJSONArray(type);
                List<SearchItem> typedItems = new ArrayList<>();
                if (array != null) {
                    for (Object obj : array) {
                        JSONObject item = (JSONObject) obj;
                        SearchItem parsed = new SearchItem()
                                .origin(item)
                                .title(PicImageSearchUtil.stringAt(item, "urlList[0].title"))
                                .url(PicImageSearchUtil.stringAt(item, "urlList[0].sourceUrl"))
                                .thumbnail(item.getString("proxyUrl"))
                                .similarity(PicImageSearchUtil.round(item.getDoubleValue("distance") * 100D, 2))
                                .put("hash", item.get("hash"))
                                .put("url_list", item.get("urlList"))
                                .put("width", item.get("width"))
                                .put("height", item.get("height"));
                        typedItems.add(parsed);
                        response.add(parsed);
                    }
                }
                response.put(type, typedItems);
            }
            return response;
        }
    }
