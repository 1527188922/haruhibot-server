package com.haruhi.botServer.picimagesearch.engine;

import com.haruhi.botServer.picimagesearch.*;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;


@Component
public class Iqdb extends AbstractSearchEngine {
        private final boolean is3d;

        public Iqdb() {
            this(false);
        }

        protected Iqdb(boolean is3d) {
            super(is3d ? "https://3d.iqdb.org" : "https://iqdb.org");
            this.is3d = is3d;
        }

        @Override
        public EngineType type() {
            return is3d ? EngineType.IQDB_3D : EngineType.IQDB;
        }

        @Override
        public SearchResponse search(SearchInput input) {
            Map<String, Object> form = new LinkedHashMap<>();
            Map<String, Object> files = null;
            if (input.hasUrl()) {
                form.put("url", input.url().orElseThrow());
            } else if (input.hasFile()) {
                files = Map.of("file", input.file().orElseThrow());
            } else {
                throw new IllegalArgumentException("Either url or file must be provided");
            }
            HttpData http = postForm("", null, form, files);
            Document document = Jsoup.parse(http.body(), http.url());
            SearchResponse response = new SearchResponse(type(), http.url(), http.body(), http.statusCode());
            for (Element table : document.select("#pages > div > table")) {
                SearchItem item = iqdbItem(table);
                if (StringUtils.isNotBlank(item.getTitle()) || StringUtils.isNotBlank(item.getUrl())) {
                    response.add(item);
                }
            }
            response.put("more", document.select("#more1 > div.pages > div > table").stream().map(this::iqdbItem).toList());
            response.put("saucenao_url", PicImageSearchUtil.firstHrefByText(document, "SauceNao", "https:"));
            response.put("ascii2d_url", PicImageSearchUtil.firstHrefByText(document, "ascii2d.net", ""));
            response.put("google_url", PicImageSearchUtil.firstHrefByText(document, "Google Images", "https:"));
            response.put("tineye_url", PicImageSearchUtil.firstHrefByText(document, "TinEye", "https:"));
            return response;
        }

        private SearchItem iqdbItem(Element table) {
            Elements rows = table.select("tr");
            String content = Optional.ofNullable(table.selectFirst("th")).map(Element::text).orElse("");
            if ("No relevant matches".equals(content) || rows.isEmpty()) {
                return new SearchItem().origin(table.outerHtml()).title(content);
            }
            Element link = table.selectFirst("td > a[href]");
            Element img = table.selectFirst("td > a > img");
            String size = rows.size() > 2 ? rows.get(2).text() : "";
            String similarityRaw = rows.size() > 3 ? rows.get(3).text() : "";
            double similarity = PicImageSearchUtil.numberFromText(similarityRaw);
            return new SearchItem()
                    .origin(table.outerHtml())
                    .title(PicImageSearchUtil.firstNonBlank(content, rows.size() > 1 ? rows.get(1).text() : ""))
                    .url(link == null ? "" : PicImageSearchUtil.ensureHttps(link.attr("href")))
                    .thumbnail(img == null ? "" : "https://iqdb.org" + img.attr("src"))
                    .source(rows.size() > 1 ? rows.get(1).text() : "")
                    .similarity(similarity)
                    .put("size", size);
        }
    }
