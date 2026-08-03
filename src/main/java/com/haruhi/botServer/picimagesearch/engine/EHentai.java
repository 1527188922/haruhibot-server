package com.haruhi.botServer.picimagesearch.engine;

import com.haruhi.botServer.picimagesearch.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;


@Component
public class EHentai extends AbstractSearchEngine {
        private final boolean isEx;
        private boolean covers;
        private boolean similar = true;
        private boolean exp;

        public EHentai() {
            this(false);
        }

        protected EHentai(boolean isEx) {
            super(isEx ? "https://upld.exhentai.org" : "https://upld.e-hentai.org");
            this.isEx = isEx;
        }

        public EHentai covers(boolean covers) {
            this.covers = covers;
            return this;
        }

        public EHentai similar(boolean similar) {
            this.similar = similar;
            return this;
        }

        public EHentai exp(boolean exp) {
            this.exp = exp;
            return this;
        }

        @Override
        public EngineType type() {
            return isEx ? EngineType.EXHENTAI : EngineType.EHENTAI;
        }

        @Override
        public SearchResponse search(SearchInput input) {
            Map<String, Object> form = new LinkedHashMap<>();
            form.put("f_sfile", "File Search");
            if (covers) {
                form.put("fs_covers", "on");
            }
            if (similar) {
                form.put("fs_similar", "on");
            }
            if (exp) {
                form.put("fs_exp", "on");
            }
            Map<String, Object> files;
            if (input.hasUrl()) {
                files = Map.of("sfile", download(input.url().orElseThrow()));
            } else if (input.hasFile()) {
                files = Map.of("sfile", input.file().orElseThrow());
            } else {
                throw new IllegalArgumentException("Either url or file must be provided");
            }
            HttpData http = postForm(isEx ? "upld/image_lookup.php" : "image_lookup.php", null, form, files);
            return parseEHentai(http.body(), http.url(), http.statusCode());
        }

        private SearchResponse parseEHentai(String html, String url, int statusCode) {
            Document document = Jsoup.parse(html, url);
            SearchResponse response = new SearchResponse(EngineType.EHENTAI, url, html, statusCode);
            if (html.contains("No unfiltered results")) {
                return response;
            }
            Elements rows = document.select(".itg > tr");
            if (rows.isEmpty()) {
                rows = document.select(".itg > .gl1t");
            }
            for (Element row : rows) {
                if (row.select("td").isEmpty() && !row.hasClass("gl1t")) {
                    continue;
                }
                Element glink = row.selectFirst(".glink");
                Element thumb = firstElement(row, ".glthumb img", ".gl1e img", ".gl3t img");
                Element type = firstElement(row, ".cs", ".cn");
                List<String> tags = row.select("div.gt,div.gtl").eachAttr("title");
                response.add(new SearchItem()
                        .origin(row.outerHtml())
                        .title(glink == null ? "" : glink.text())
                        .url(glink == null ? "" : glink.parents().select("a[href]").stream().findFirst().map(a -> a.absUrl("href")).orElse(""))
                        .thumbnail(thumb == null ? "" : PicImageSearchUtil.firstNonBlank(thumb.attr("data-src"), thumb.absUrl("src"), thumb.attr("src")))
                        .source(type == null ? "" : type.text())
                        .put("date", Optional.ofNullable(row.selectFirst("[id^=posted]")).map(Element::text).orElse(""))
                        .put("tags", tags));
            }
            return response;
        }

        private Element firstElement(Element root, String... selectors) {
            for (String selector : selectors) {
                Element element = root.selectFirst(selector);
                if (element != null) {
                    return element;
                }
            }
            return null;
        }
    }
