package com.haruhi.botServer.picimagesearch.engine;

import com.haruhi.botServer.picimagesearch.*;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;


@Component
public class Google extends AbstractSearchEngine {
        public Google() {
            super("https://www.google.com/searchbyimage");
        }

        @Override
        public EngineType type() {
            return EngineType.GOOGLE;
        }

        @Override
        public SearchResponse search(SearchInput input) {
            Map<String, Object> params = new LinkedHashMap<>();
            params.put("sbisrc", 1);
            params.put("safe", "off");
            HttpData http;
            if (input.hasUrl()) {
                params.put("image_url", input.url().orElseThrow());
                http = get("", params);
            } else if (input.hasFile()) {
                http = postForm("upload", null, params, Map.of("encoded_image", input.file().orElseThrow()));
            } else {
                throw new IllegalArgumentException("Either url or file must be provided");
            }
            return parseGoogle(http.body(), http.url(), http.statusCode());
        }

        private SearchResponse parseGoogle(String html, String url, int statusCode) {
            Document document = Jsoup.parse(html, url);
            SearchResponse response = new SearchResponse(EngineType.GOOGLE, url, html, statusCode);
            for (Element g : document.select("div.g, div[data-hveid]")) {
                Element link = g.selectFirst("a[href]");
                if (link == null) {
                    continue;
                }
                String href = link.absUrl("href");
                String title = Optional.ofNullable(g.selectFirst("h3")).map(Element::text).orElse(link.text());
                String thumbnail = Optional.ofNullable(g.selectFirst("img")).map(img -> img.absUrl("src")).orElse("");
                if (StringUtils.isNotBlank(title) || StringUtils.isNotBlank(thumbnail)) {
                    response.add(new SearchItem().origin(g.outerHtml()).title(title).url(href).thumbnail(thumbnail));
                }
            }
            for (Element page : document.select("a[aria-label^=Page], a.fl")) {
                response.put("page:" + page.text(), page.absUrl("href"));
            }
            return response;
        }
    }
