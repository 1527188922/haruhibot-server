package com.haruhi.botServer.picimagesearch.engine;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.haruhi.botServer.picimagesearch.*;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class Baidu extends AbstractSearchEngine {
        public Baidu() {
            super("https://graph.baidu.com");
        }

        @Override
        public EngineType type() {
            return EngineType.BAIDU;
        }

        @Override
        public SearchResponse search(SearchInput input) {
            Map<String, Object> files;
            if (input.hasUrl()) {
                files = Map.of("image", download(input.url().orElseThrow()));
            } else if (input.hasFile()) {
                files = Map.of("image", input.file().orElseThrow());
            } else {
                throw new IllegalArgumentException("Either url or file must be provided");
            }
            HttpData upload = postForm("upload", null, Map.of("from", "pc"), files);
            JSONObject uploadJson = PicImageSearchUtil.parseJson(upload.body());
            String dataUrl = PicImageSearchUtil.stringAt(uploadJson, "data.url");
            if (StringUtils.isBlank(dataUrl)) {
                return new SearchResponse(type(), upload.url(), uploadJson, upload.statusCode());
            }
            HttpData page = get(dataUrl, null);
            List<Object> cardData = extractBaiduCardData(page.body());
            JSONObject sameData = null;
            for (Object obj : cardData) {
                JSONObject card = (JSONObject) obj;
                if ("noresult".equals(card.getString("cardName"))) {
                    return new SearchResponse(type(), dataUrl, cardData, page.statusCode());
                }
                if ("same".equals(card.getString("cardName"))) {
                    sameData = card.getJSONObject("tplData");
                }
                if ("simipic".equals(card.getString("cardName"))) {
                    String nextUrl = PicImageSearchUtil.stringAt(card, "tplData.firstUrl");
                    HttpData next = get(nextUrl, null);
                    JSONObject json = PicImageSearchUtil.parseJson(next.body());
                    if (sameData != null) {
                        json.put("same", sameData);
                    }
                    return parseBaidu(json, dataUrl, next.statusCode());
                }
            }
            return new SearchResponse(type(), dataUrl, cardData, page.statusCode());
        }

        private SearchResponse parseBaidu(JSONObject json, String url, int statusCode) {
            SearchResponse response = new SearchResponse(EngineType.BAIDU, url, json, statusCode);
            JSONArray exact = PicImageSearchUtil.jsonAtArray(json, "same.list");
            if (exact != null) {
                response.put("exact_matches", parseBaiduItems(exact));
            }
            JSONArray data = PicImageSearchUtil.jsonAtArray(json, "data.list");
            if (data != null) {
                parseBaiduItems(data).forEach(response::add);
            }
            return response;
        }

        private List<SearchItem> parseBaiduItems(JSONArray data) {
            List<SearchItem> items = new ArrayList<>();
            for (Object obj : data) {
                JSONObject item = (JSONObject) obj;
                items.add(new SearchItem()
                        .origin(item)
                        .title(PicImageSearchUtil.firstString(item, "title"))
                        .thumbnail(PicImageSearchUtil.firstString(item, "image_src", "thumbUrl"))
                        .url(PicImageSearchUtil.firstString(item, "url", "fromUrl")));
            }
            return items;
        }

        private List<Object> extractBaiduCardData(String html) {
            Document document = Jsoup.parse(html);
            for (Element script : document.select("script")) {
                String text = script.html();
                if (text.contains("window.cardData")) {
                    int start = text.indexOf('[');
                    int end = text.lastIndexOf(']') + 1;
                    if (start >= 0 && end > start) {
                        return JSONArray.parseArray(text.substring(start, end));
                    }
                }
            }
            return List.of();
        }
    }
