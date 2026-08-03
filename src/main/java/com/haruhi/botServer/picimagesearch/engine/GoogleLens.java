package com.haruhi.botServer.picimagesearch.engine;

import com.haruhi.botServer.picimagesearch.*;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class GoogleLens extends AbstractSearchEngine {
        private String searchType = "all";
        private String q;
        private String hl = "en";
        private String country = "US";

        public GoogleLens() {
            super("https://lens.google.com");
        }

        public GoogleLens searchType(String searchType) {
            this.searchType = searchType;
            return this;
        }

        public GoogleLens q(String q) {
            this.q = q;
            return this;
        }

        @Override
        public EngineType type() {
            return EngineType.GOOGLE_LENS;
        }

        @Override
        public SearchResponse search(SearchInput input) {
            Map<String, Object> params = new LinkedHashMap<>();
            params.put("hl", hl + "-" + country.toUpperCase());
            if (StringUtils.isNotBlank(q) && !"exact_matches".equals(searchType)) {
                params.put("q", q);
            }
            HttpData http;
            if (input.hasFile()) {
                http = postForm("v3/upload", params, null, Map.of("encoded_image", input.file().orElseThrow()));
            } else if (input.hasUrl()) {
                params.put("url", input.url().orElseThrow());
                http = get("uploadbyurl", params);
            } else {
                throw new IllegalArgumentException("Either url or file must be provided");
            }
            Document document = Jsoup.parse(http.body(), http.url());
            if (!"all".equals(searchType)) {
                String udm = Map.of("products", "37", "visual_matches", "44", "exact_matches", "48")
                        .get(searchType);
                if (udm != null) {
                    Element link = document.selectFirst("a[href*=udm=" + udm + "]");
                    if (link != null) {
                        http = get("https://www.google.com" + link.attr("href"), null);
                    }
                }
            }
            return parseGoogleLens(http.body(), http.url(), http.statusCode());
        }

        private SearchResponse parseGoogleLens(String html, String url, int statusCode) {
            Document document = Jsoup.parse(html, url);
            SearchResponse response = new SearchResponse(EngineType.GOOGLE_LENS, url, html, statusCode);
            Map<String, String> imageMap = extractLensImageMap(html);
            for (Element link : document.select("a[href]")) {
                String href = link.absUrl("href");
                if (StringUtils.isBlank(href) || href.contains("google.com/search")) {
                    continue;
                }
                Element parent = nearestResultContainer(link);
                String title = PicImageSearchUtil.firstNonBlank(link.text(), parent == null ? "" : parent.text());
                String thumbnail = parent == null ? "" : Optional.ofNullable(parent.selectFirst("img"))
                        .map(img -> PicImageSearchUtil.firstNonBlank(img.absUrl("src"), imageMap.get(img.attr("id")), img.attr("src")))
                        .orElse("");
                if (StringUtils.isNotBlank(title) && StringUtils.isNotBlank(href)) {
                    response.add(new SearchItem()
                            .origin(parent == null ? link.outerHtml() : parent.outerHtml())
                            .title(title)
                            .url(href)
                            .thumbnail(thumbnail)
                            .source(siteName(href)));
                }
            }
            return response;
        }

        private Map<String, String> extractLensImageMap(String html) {
            Map<String, String> result = new HashMap<>();
            Matcher matcher = Pattern.compile("\\[\\\"(dimg_[^\\\"]+)\\\",\\\"(https?[^\\\"]+)\\\"").matcher(html);
            while (matcher.find()) {
                result.put(matcher.group(1), matcher.group(2).replace("\\u003d", "=").replace("\\u0026", "&"));
            }
            return result;
        }

        private Element nearestResultContainer(Element element) {
            Element current = element;
            for (int i = 0; i < 4 && current != null; i++) {
                if (current.selectFirst("img") != null) {
                    return current;
                }
                current = current.parent();
            }
            return element.parent();
        }

        private String siteName(String url) {
            try {
                return URI.create(url).getHost();
            } catch (Exception e) {
                return "";
            }
        }
    }
