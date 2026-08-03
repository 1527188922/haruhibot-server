package com.haruhi.botServer.picimagesearch.engine;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.haruhi.botServer.picimagesearch.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


@Component
public class SauceNao extends AbstractSearchEngine {
        private String apiKey;
        private int numres = 5;
        private int hide = 0;
        private int minsim = 30;
        private int outputType = 2;
        private int testmode = 0;
        private int db = 999;
        private List<Integer> dbs = new ArrayList<>();

        public SauceNao() {
            super("https://saucenao.com/search.php");
        }

        public SauceNao apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        public SauceNao numres(int numres) {
            this.numres = numres;
            return this;
        }

        public SauceNao testmode(int testmode) {
            this.testmode = testmode;
            return this;
        }

        public SauceNao db(int db) {
            this.db = db;
            return this;
        }

        public SauceNao dbs(Integer... dbs) {
            this.dbs = Arrays.asList(dbs);
            return this;
        }

        @Override
        public EngineType type() {
            return EngineType.SAUCENAO;
        }

        @Override
        public SearchResponse search(SearchInput input) {
            Map<String, Object> params = new LinkedHashMap<>();
            params.put("testmode", testmode);
            params.put("numres", numres);
            params.put("output_type", outputType);
            params.put("hide", hide);
            params.put("minsim", minsim);
            if (StringUtils.isNotBlank(apiKey)) {
                params.put("api_key", apiKey);
            }
            if (dbs.isEmpty()) {
                params.put("db", db);
            } else {
                for (Integer item : dbs) {
                    params.put("dbs[]", item);
                }
            }
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
            SearchResponse response = new SearchResponse(type(), sauceNaoResultUrl(json), json, http.statusCode());
            JSONObject header = json.getJSONObject("header");
            if (header != null) {
                response.put("header", header);
            }
            for (Object obj : json.getJSONArray("results") == null ? Collections.emptyList() : json.getJSONArray("results")) {
                JSONObject result = (JSONObject) obj;
                JSONObject data = result.getJSONObject("data");
                JSONObject itemHeader = result.getJSONObject("header");
                response.add(new SearchItem()
                        .origin(result)
                        .similarity(itemHeader == null ? 0D : itemHeader.getDoubleValue("similarity"))
                        .thumbnail(itemHeader == null ? "" : itemHeader.getString("thumbnail"))
                        .title(PicImageSearchUtil.firstString(data, "title", "material", "jp_name", "eng_name", "source", "created_at"))
                        .url(sauceNaoItemUrl(data))
                        .author(sauceNaoAuthor(data))
                        .authorUrl(sauceNaoAuthorUrl(data))
                        .source(data == null ? "" : data.getString("source"))
                        .put("index_id", itemHeader == null ? null : itemHeader.get("index_id"))
                        .put("index_name", itemHeader == null ? null : itemHeader.get("index_name"))
                        .put("ext_urls", data == null ? null : data.get("ext_urls")));
            }
            return response;
        }

        private String sauceNaoResultUrl(JSONObject json) {
            String display = PicImageSearchUtil.stringAt(json, "header.query_image_display");
            if (StringUtils.isBlank(display)) {
                return "https://saucenao.com/search.php";
            }
            return "https://saucenao.com/search.php?url=https://saucenao.com" + display;
        }

        private String sauceNaoItemUrl(JSONObject data) {
            if (data == null) {
                return "";
            }
            if (data.containsKey("pixiv_id")) {
                return "https://www.pixiv.net/artworks/" + data.getString("pixiv_id");
            }
            if (data.containsKey("pawoo_id")) {
                return "https://pawoo.net/@" + data.getString("pawoo_user_acct") + "/" + data.getString("pawoo_id");
            }
            if (data.containsKey("getchu_id")) {
                return "https://www.getchu.com/soft.phtml?id=" + data.getString("getchu_id");
            }
            JSONArray extUrls = data.getJSONArray("ext_urls");
            return extUrls == null || extUrls.isEmpty() ? "" : extUrls.getString(0);
        }

        private String sauceNaoAuthor(JSONObject data) {
            if (data == null) {
                return "";
            }
            for (String key : List.of("author", "member_name", "twitter_user_handle", "pawoo_user_display_name",
                    "author_name", "user_name", "artist", "company")) {
                if (StringUtils.isNotBlank(data.getString(key))) {
                    return data.getString(key);
                }
            }
            Object creator = data.get("creator");
            if (creator instanceof JSONArray array) {
                return String.join(", ", array.toJavaList(String.class));
            }
            return creator == null ? "" : creator.toString();
        }

        private String sauceNaoAuthorUrl(JSONObject data) {
            if (data == null) {
                return "";
            }
            if (data.containsKey("pixiv_id")) {
                return "https://www.pixiv.net/users/" + data.getString("member_id");
            }
            if (data.containsKey("seiga_id")) {
                return "https://seiga.nicovideo.jp/user/illust/" + data.getString("member_id");
            }
            if (data.containsKey("nijie_id")) {
                return "https://nijie.info/members.php?id=" + data.getString("member_id");
            }
            if (data.containsKey("bcy_id")) {
                return "https://bcy.net/u/" + data.getString("member_id");
            }
            if (data.containsKey("tweet_id")) {
                return "https://twitter.com/intent/user?user_id=" + data.getString("twitter_user_id");
            }
            if (data.containsKey("pawoo_user_acct")) {
                return "https://pawoo.net/@" + data.getString("pawoo_user_acct");
            }
            return data.getString("author_url");
        }
    }
