package com.haruhi.botServer.picimagesearch.engine;

import com.haruhi.botServer.picimagesearch.*;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;


@Component
public class Ascii2D extends AbstractSearchEngine {
        private final boolean bovw;

        public Ascii2D() {
            this(false);
        }

        protected Ascii2D(boolean bovw) {
            super("https://ascii2d.net/search");
            this.bovw = bovw;
        }

        @Override
        public EngineType type() {
            return bovw ? EngineType.ASCII2D_BOVW : EngineType.ASCII2D;
        }

        @Override
        public SearchResponse search(SearchInput input) {
            HttpData http;
            if (input.hasUrl()) {
                http = postForm("uri", null, Map.of("uri", input.url().orElseThrow()), null);
            } else if (input.hasFile()) {
                http = postForm("file", null, null, Map.of("file", input.file().orElseThrow()));
            } else {
                throw new IllegalArgumentException("Either url or file must be provided");
            }
            if (bovw) {
                http = get(http.url().replace("/color/", "/bovw/"), null);
            }
            Document document = Jsoup.parse(http.body(), http.url());
            SearchResponse response = new SearchResponse(type(), http.url(), http.body(), http.statusCode());
            for (Element row : document.select("div.row.item-box")) {
                response.add(ascii2dItem(row));
            }
            return response;
        }

        private SearchItem ascii2dItem(Element row) {
            Element img = row.selectFirst("img");
            Elements links = row.select("div.detail-box.gray-link a");
            String title = "";
            String url = "";
            String author = "";
            String authorUrl = "";
            if (!links.isEmpty()) {
                title = links.get(0).text();
                url = links.get(0).absUrl("href");
                if (links.size() > 1) {
                    author = links.get(1).text();
                    authorUrl = links.get(1).absUrl("href");
                }
            }
            if (StringUtils.isBlank(title)) {
                Element h6 = row.selectFirst("div.detail-box.gray-link h6");
                title = h6 == null ? row.select("div.detail-box.gray-link").text() : h6.text();
            }
            List<Map<String, String>> urlList = new ArrayList<>();
            for (Element link : links) {
                urlList.add(Map.of("href", link.absUrl("href"), "text", link.text()));
            }
            return new SearchItem()
                    .origin(row.outerHtml())
                    .title(title)
                    .url(url)
                    .thumbnail(img == null ? "" : PicImageSearchUtil.firstNonBlank(img.absUrl("src"), img.attr("src")))
                    .author(author)
                    .authorUrl(authorUrl)
                    .put("hash", Optional.ofNullable(row.selectFirst("div.hash")).map(Element::text).orElse(""))
                    .put("detail", Optional.ofNullable(row.selectFirst("small")).map(Element::text).orElse(""))
                    .put("url_list", urlList);
        }
    }
