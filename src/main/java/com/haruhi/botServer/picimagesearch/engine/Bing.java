package com.haruhi.botServer.picimagesearch.engine;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.URLUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.haruhi.botServer.picimagesearch.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;



@Component
public class Bing extends AbstractSearchEngine {
        private static final String BING_SIGNATURE_KEY =
                "AAAAC3NzaC1lZDI1NTE5AAAAIGd3gMN2v1KRLBGmotz7jbQYF8PaB+Jpe6iVf2YIeN5b";
        private static final Pattern SKEY = Pattern.compile("skey=([^&]+)");
        private static final Pattern IMAGE_SIGNATURE = Pattern.compile("imageSignature&quot;:&quot;(.+?)&quot;");
        private static final Pattern BCID = Pattern.compile("(bcid_[A-Za-z0-9-.]+)");
        private String sessionKey;
        private String imageSignature;

        public Bing() {
            super("https://www.bing.com");
        }

        @Override
        public EngineType type() {
            return EngineType.BING;
        }

        @Override
        public SearchResponse search(SearchInput input) {
            String respUrl;
            JSONObject json;
            if (input.hasUrl()) {
                String imageUrl = input.url().orElseThrow();
                respUrl = baseUrl + "/images/search?view=detailv2&iss=sbi&FORM=SBIHMP&sbisrc=UrlPaste&q=imgurl:"
                        + URLUtil.encodeAll(imageUrl) + "&idpbck=1";
                HttpData page = get(respUrl, null);
                imageSignature = PicImageSearchUtil.regex(page.body(), IMAGE_SIGNATURE);
                json = getInsights(null, imageUrl);
            } else if (input.hasFile()) {
                UploadResult upload = uploadImage(input.file().orElseThrow());
                respUrl = upload.responseUrl();
                json = getInsights(upload.bcid(), null);
            } else {
                throw new IllegalArgumentException("Either url or file must be provided");
            }
            return parseBing(json, respUrl, 200);
        }

        private UploadResult uploadImage(File file) {
            Map<String, Object> files = new LinkedHashMap<>();
            files.put("cbir", "sbi");
            files.put("imageBin", Base64.encode(file));
            HttpData http = postForm("images/search?view=detailv2&iss=sbiupload", null, null, files);
            sessionKey = PicImageSearchUtil.regex(http.body(), SKEY);
            imageSignature = PicImageSearchUtil.regex(http.body(), IMAGE_SIGNATURE);
            String bcid = PicImageSearchUtil.regex(http.body(), BCID);
            if (StringUtils.isBlank(bcid)) {
                throw new PicImageSearchException("BCID not found on page");
            }
            return new UploadResult(bcid, http.url());
        }

        private JSONObject getInsights(String bcid, String imageUrl) {
            Map<String, Object> params = new LinkedHashMap<>();
            params.put("rshighlight", "true");
            params.put("textDecorations", "true");
            params.put("internalFeatures", "similarproducts,share");
            params.put("nbl", "1");
            params.put("skey", sessionKey);
            params.put("safeSearch", "off");
            params.put("mkt", "en-us");
            params.put("setLang", "en-us");
            params.put("iss", "SBIUPLOADGET");
            params.put("IID", "idpins");
            params.put("SFX", "1");
            String referer;
            JSONObject imageInfo = new JSONObject();
            JSONObject nested = new JSONObject();
            if (StringUtils.isNotBlank(imageUrl)) {
                referer = baseUrl + "/images/search?view=detailv2&iss=sbi&FORM=SBIHMP&sbisrc=UrlPaste&q=imgurl:"
                        + URLUtil.encodeAll(imageUrl) + "&idpbck=1";
                nested.put("url", imageUrl);
                nested.put("source", "Url");
            } else {
                params.put("insightsToken", bcid);
                referer = baseUrl + "/images/search?insightsToken=" + bcid;
                nested.put("imageInsightsToken", bcid);
                nested.put("source", "Gallery");
            }
            imageInfo.put("imageInfo", nested);
            Map<String, Object> form = Map.of("knowledgeRequest", JSONObject.toJSONString(imageInfo));
            Map<String, String> oldHeaders = new LinkedHashMap<>(headers);
            headers.put("Referer", referer);
            if (StringUtils.isNotBlank(imageSignature)) {
                headers.put("X-Image-Knowledge-Signature", parseBingSignature(imageSignature));
            }
            HttpData http = postForm("images/api/custom/knowledge", params, form, null);
            headers = oldHeaders;
            return PicImageSearchUtil.parseJson(http.body());
        }

        private SearchResponse parseBing(JSONObject json, String url, int statusCode) {
            SearchResponse response = new SearchResponse(EngineType.BING, url, json, statusCode);
            JSONArray tags = json.getJSONArray("tags");
            if (tags == null) {
                return response;
            }
            List<SearchItem> pagesIncluding = new ArrayList<>();
            List<SearchItem> visualSearch = new ArrayList<>();
            List<SearchItem> relatedSearches = new ArrayList<>();
            for (Object tagObj : tags) {
                JSONArray actions = ((JSONObject) tagObj).getJSONArray("actions");
                if (actions == null) {
                    continue;
                }
                for (Object actionObj : actions) {
                    JSONObject action = (JSONObject) actionObj;
                    String actionType = action.getString("actionType");
                    if ("BestRepresentativeQuery".equals(actionType)) {
                        response.put("best_guess", action.getString("displayName"));
                    } else if ("PagesIncluding".equals(actionType)) {
                        pagesIncluding.addAll(parseBingImageItems(PicImageSearchUtil.jsonAtArray(action, "data.value")));
                    } else if ("VisualSearch".equals(actionType)) {
                        visualSearch.addAll(parseBingImageItems(PicImageSearchUtil.jsonAtArray(action, "data.value")));
                    } else if ("RelatedSearches".equals(actionType)) {
                        relatedSearches.addAll(parseBingRelatedItems(PicImageSearchUtil.jsonAtArray(action, "data.value")));
                    } else if ("Entity".equals(actionType)) {
                        response.put("entity", action.get("data"));
                    } else if ("Travel".equals(actionType)) {
                        response.put("travel", action.get("data"));
                    }
                }
            }
            pagesIncluding.forEach(response::add);
            response.put("pages_including", pagesIncluding);
            response.put("visual_search", visualSearch);
            response.put("related_searches", relatedSearches);
            return response;
        }

        private List<SearchItem> parseBingImageItems(JSONArray values) {
            if (values == null) {
                return List.of();
            }
            List<SearchItem> items = new ArrayList<>();
            for (Object obj : values) {
                JSONObject item = (JSONObject) obj;
                items.add(new SearchItem()
                        .origin(item)
                        .title(item.getString("name"))
                        .url(item.getString("hostPageUrl"))
                        .thumbnail(item.getString("thumbnailUrl"))
                        .imageUrl(item.getString("contentUrl")));
            }
            return items;
        }

        private List<SearchItem> parseBingRelatedItems(JSONArray values) {
            if (values == null) {
                return List.of();
            }
            List<SearchItem> items = new ArrayList<>();
            for (Object obj : values) {
                JSONObject item = (JSONObject) obj;
                items.add(new SearchItem()
                        .origin(item)
                        .title(item.getString("text"))
                        .thumbnail(PicImageSearchUtil.stringAt(item, "thumbnail.url")));
            }
            return items;
        }

        private String parseBingSignature(String rawSignature) {
            String[] parts = rawSignature.split("\\|");
            if (parts.length != 3) {
                return rawSignature;
            }
            byte[] decoded;
            try {
                decoded = java.util.Base64.getDecoder().decode(parts[1]);
            } catch (Exception e) {
                return rawSignature;
            }
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < decoded.length; i++) {
                int keyChar = BING_SIGNATURE_KEY.charAt(i % BING_SIGNATURE_KEY.length());
                builder.append((char) ((decoded[i] ^ keyChar) - 3));
            }
            return parts[0] + "|" + builder + "|" + parts[2];
        }
    }
