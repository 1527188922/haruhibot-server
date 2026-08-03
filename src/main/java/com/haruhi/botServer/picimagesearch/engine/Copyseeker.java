package com.haruhi.botServer.picimagesearch.engine;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.haruhi.botServer.picimagesearch.*;
import org.apache.commons.lang3.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class Copyseeker extends AbstractSearchEngine {
        private static final String URL_SEARCH_TOKEN = "40550e4a7aa709f3b9d811652f4709bc532ddf7ff7";
        private static final String FILE_UPLOAD_TOKEN = "400e93c447183722643372296f66041a6bc12578b8";
        private static final String SET_COOKIE_TOKEN = "001d97c0b67ff86415a5834958bf47b1720f17d286";
        private static final String GET_RESULTS_TOKEN = "40625aa15a7e11b250e74d850fe96cd50b2d7eca11";

        public Copyseeker() {
            super("https://copyseeker.net");
        }

        @Override
        public EngineType type() {
            return EngineType.COPYSEEKER;
        }

        @Override
        public SearchResponse search(SearchInput input) {
            String discoveryId = discoveryId(input);
            if (StringUtils.isBlank(discoveryId)) {
                return new SearchResponse(type(), "", new JSONObject(), 200);
            }
            Map<String, Object> payload = Map.of("discoveryId", discoveryId, "hasBlocker", false);
            headers.put("next-action", GET_RESULTS_TOKEN);
            HttpData http = postJson("discovery", null, List.of(payload));
            headers.remove("next-action");
            JSONObject json = parseNextActionJson(http.body());
            return parseCopyseeker(json, http.url(), http.statusCode());
        }

        private String discoveryId(SearchInput input) {
            headers.put("content-type", "text/plain;charset=UTF-8");
            headers.put("next-action", SET_COOKIE_TOKEN);
            postForm("", null, null, null);
            headers.remove("content-type");
            HttpData http;
            if (input.hasUrl()) {
                headers.put("next-action", URL_SEARCH_TOKEN);
                http = postJson("", null, List.of(Map.of("discoveryType", "ReverseImageSearch", "imageUrl", input.url().orElseThrow())));
            } else if (input.hasFile()) {
                headers.put("next-action", FILE_UPLOAD_TOKEN);
                Map<String, Object> files = new LinkedHashMap<>();
                files.put("1_file", input.file().orElseThrow());
                files.put("1_discoveryType", "ReverseImageSearch");
                files.put("0", "[\"$K1\"]");
                http = postForm("", null, null, files);
            } else {
                throw new IllegalArgumentException("Either url or file must be provided");
            }
            headers.remove("next-action");
            JSONObject json = parseNextActionJson(http.body());
            return json.getString("discoveryId");
        }

        private SearchResponse parseCopyseeker(JSONObject json, String url, int statusCode) {
            SearchResponse response = new SearchResponse(EngineType.COPYSEEKER, url, json, statusCode)
                    .put("id", json.get("id"))
                    .put("image_url", json.get("imageUrl"))
                    .put("best_guess_label", json.get("bestGuessLabel"))
                    .put("entities", json.get("entities"))
                    .put("total", json.get("totalLinksFound"))
                    .put("exif", json.get("exif"))
                    .put("similar_image_urls", json.get("visuallySimilarImages"));
            JSONArray pages = json.getJSONArray("pages");
            if (pages != null) {
                for (Object obj : pages) {
                    JSONObject item = (JSONObject) obj;
                    response.add(new SearchItem()
                            .origin(item)
                            .title(item.getString("title"))
                            .url(item.getString("url"))
                            .thumbnail(item.getString("mainImage"))
                            .put("thumbnail_list", item.get("otherImages"))
                            .put("website_rank", item.get("rank")));
                }
            }
            return response;
        }

        private JSONObject parseNextActionJson(String body) {
            for (String line : body.split("\\R")) {
                String trimmed = line.trim();
                if (trimmed.startsWith("1:{")) {
                    return PicImageSearchUtil.parseJson(trimmed.substring(2));
                }
            }
            return new JSONObject();
        }
    }
