package com.haruhi.botServer.picimagesearch.engine;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.haruhi.botServer.picimagesearch.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class Tineye extends AbstractSearchEngine {
        public Tineye() {
            super("https://tineye.com");
        }

        @Override
        public EngineType type() {
            return EngineType.TINEYE;
        }

        @Override
        public SearchResponse search(SearchInput input) {
            Map<String, Object> params = new LinkedHashMap<>();
            params.put("sort", "score");
            params.put("order", "desc");
            params.put("page", 1);
            Map<String, Object> files = null;
            if (input.hasUrl()) {
                params.put("url", input.url().orElseThrow());
            } else if (input.hasFile()) {
                files = Map.of("image", input.file().orElseThrow());
            } else {
                throw new IllegalArgumentException("Either url or file must be provided");
            }
            HttpData http = postForm("api/v1/result_json/", null, params, files);
            JSONObject json = PicImageSearchUtil.parseJson(http.body());
            SearchResponse response = parseTineye(json, http.url(), http.statusCode());
            String queryHash = PicImageSearchUtil.stringAt(json, "query.hash");
            if (StringUtils.isNotBlank(queryHash)) {
                HttpData domainsResp = get("api/v1/search/get_domains/" + queryHash, null);
                JSONObject domainsJson = PicImageSearchUtil.parseJson(domainsResp.body());
                response.put("domains", parseDomainInfo(domainsJson.getJSONArray("domains")));
            }
            return response;
        }

        private SearchResponse parseTineye(JSONObject json, String url, int statusCode) {
            SearchResponse response = new SearchResponse(EngineType.TINEYE, url, json, statusCode)
                    .put("query_hash", json.get("query_hash"))
                    .put("total_pages", json.get("total_pages"));
            JSONArray matches = json.getJSONArray("matches");
            if (matches != null) {
                for (Object obj : matches) {
                    JSONObject item = (JSONObject) obj;
                    JSONObject backlink = null;
                    JSONArray backlinks = item.getJSONArray("backlinks");
                    if (backlinks != null && !backlinks.isEmpty()) {
                        backlink = backlinks.getJSONObject(0);
                    }
                    response.add(new SearchItem()
                            .origin(item)
                            .thumbnail(item.getString("image_url"))
                            .imageUrl(backlink == null ? "" : backlink.getString("url"))
                            .url(backlink == null ? "" : backlink.getString("backlink"))
                            .source(item.getString("domain"))
                            .put("width", item.get("width"))
                            .put("height", item.get("height"))
                            .put("crawl_date", backlink == null ? null : backlink.get("crawl_date")));
                }
            }
            return response;
        }

        private List<DomainInfo> parseDomainInfo(JSONArray domains) {
            if (domains == null) {
                return List.of();
            }
            List<DomainInfo> results = new ArrayList<>();
            for (Object obj : domains) {
                JSONArray data = (JSONArray) obj;
                String tag = "";
                if (data.size() > 2 && data.get(2) instanceof JSONArray tagArray && !tagArray.isEmpty()) {
                    tag = tagArray.getString(0);
                }
                results.add(new DomainInfo(data.getString(0), data.getIntValue(1), tag));
            }
            return results;
        }
    }
