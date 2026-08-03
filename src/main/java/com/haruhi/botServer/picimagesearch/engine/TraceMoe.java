package com.haruhi.botServer.picimagesearch.engine;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.haruhi.botServer.picimagesearch.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class TraceMoe extends AbstractSearchEngine {
        private boolean mute;
        private String size;

        public TraceMoe() {
            super("https://api.trace.moe/search");
        }

        public TraceMoe mute(boolean mute) {
            this.mute = mute;
            return this;
        }

        public TraceMoe size(String size) {
            this.size = size;
            return this;
        }

        @Override
        public EngineType type() {
            return EngineType.TRACE_MOE;
        }

        public SearchResponse me(String key) {
            Map<String, Object> params = StringUtils.isBlank(key) ? Map.of() : Map.of("key", key);
            HttpData http = get("https://api.trace.moe/me", params);
            return new SearchResponse(type(), http.url(), PicImageSearchUtil.parseJson(http.body()), http.statusCode());
        }

        @Override
        public SearchResponse search(SearchInput input) {
            Map<String, Object> params = new LinkedHashMap<>();
            params.put("anilistInfo", "");
            params.put("cutBorders", "true");
            Map<String, Object> files = null;
            if (input.hasUrl()) {
                params.put("url", input.url().orElseThrow());
            } else if (input.hasFile()) {
                files = Map.of("file", input.file().orElseThrow());
            } else {
                throw new IllegalArgumentException("Either url or file must be provided");
            }
            HttpData http = postForm("", params, null, files);
            JSONObject json = PicImageSearchUtil.parseJson(http.body());
            SearchResponse response = new SearchResponse(type(), http.url(), json, http.statusCode())
                    .put("frameCount", json.get("frameCount"))
                    .put("error", json.getString("error"));
            JSONArray results = json.getJSONArray("result");
            if (results != null) {
                for (Object obj : results) {
                    JSONObject item = (JSONObject) obj;
                    String video = appendTraceMoePreviewParams(item.getString("video"));
                    String image = appendTraceMoePreviewParams(item.getString("image"));
                    JSONObject anilist = item.getJSONObject("anilist");
                    JSONObject title = anilist == null ? null : anilist.getJSONObject("title");
                    response.add(new SearchItem()
                            .origin(item)
                            .title(PicImageSearchUtil.firstString(title, "chinese", "native", "romaji", "english", "filename"))
                            .url("")
                            .thumbnail(image)
                            .imageUrl(image)
                            .similarity(PicImageSearchUtil.round(item.getDoubleValue("similarity") * 100D, 2))
                            .put("filename", item.getString("filename"))
                            .put("episode", item.get("episode"))
                            .put("from", item.get("from"))
                            .put("to", item.get("to"))
                            .put("video", video)
                            .put("anilist", anilist));
                }
            }
            return response;
        }

        private String appendTraceMoePreviewParams(String url) {
            if (StringUtils.isBlank(url)) {
                return "";
            }
            String result = url;
            if (Arrays.asList("l", "m", "s").contains(size)) {
                result += "&size=" + size;
            }
            if (mute) {
                result += "&mute";
            }
            return result;
        }
    }
