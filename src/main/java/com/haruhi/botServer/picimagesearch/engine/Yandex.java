package com.haruhi.botServer.picimagesearch.engine;

import com.haruhi.botServer.picimagesearch.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class Yandex extends AbstractSearchEngine {
        public Yandex() {
            super("https://yandex.com/images/search");
        }

        @Override
        public EngineType type() {
            return EngineType.YANDEX;
        }

        @Override
        public SearchResponse search(SearchInput input) {
            Map<String, Object> params = new LinkedHashMap<>();
            params.put("rpt", "imageview");
            params.put("cbir_page", "sites");
            HttpData http;
            if (input.hasUrl()) {
                params.put("url", input.url().orElseThrow());
                http = get("", params);
            } else if (input.hasFile()) {
                http = postForm("", params, Map.of("prg", 1), Map.of("upfile", input.file().orElseThrow()));
            } else {
                throw new IllegalArgumentException("Either url or file must be provided");
            }
            return parseYandex(http.body(), http.url(), http.statusCode());
        }

        private SearchResponse parseYandex(String html, String url, int statusCode) {
            Document document = Jsoup.parse(html, url);
            SearchResponse response = new SearchResponse(EngineType.YANDEX, url, html, statusCode);
            for (Element item : document.select(".CbirSites-Item, .serp-item, .CbirSimilar-Item")) {
                Element link = item.selectFirst("a[href]");
                Element img = item.selectFirst("img");
                response.add(new SearchItem()
                        .origin(item.outerHtml())
                        .title(item.text())
                        .url(link == null ? "" : link.absUrl("href"))
                        .thumbnail(img == null ? "" : PicImageSearchUtil.firstNonBlank(img.absUrl("src"), img.attr("src"))));
            }
            return response;
        }
    }
