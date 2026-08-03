package com.haruhi.botServer.picimagesearch;

import lombok.Getter;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.haruhi.botServer.picimagesearch.PicImageSearchUtil.*;


@Getter
public class SearchItem {
        private Object origin;
        private String title = "";
        private String url = "";
        private String thumbnail = "";
        private String imageUrl = "";
        private String author = "";
        private String authorUrl = "";
        private String source = "";
        private double similarity;
        private final Map<String, Object> extra = new LinkedHashMap<>();

    public SearchItem origin(Object origin) {
            this.origin = origin;
            return this;
        }

    public SearchItem title(String title) {
            this.title = value(title);
            return this;
        }

    public SearchItem url(String url) {
            this.url = value(url);
            return this;
        }

    public SearchItem thumbnail(String thumbnail) {
            this.thumbnail = value(thumbnail);
            return this;
        }

    public SearchItem imageUrl(String imageUrl) {
            this.imageUrl = value(imageUrl);
            return this;
        }

    public SearchItem author(String author) {
            this.author = value(author);
            return this;
        }

    public SearchItem authorUrl(String authorUrl) {
            this.authorUrl = value(authorUrl);
            return this;
        }

    public SearchItem source(String source) {
            this.source = value(source);
            return this;
        }

    public SearchItem similarity(double similarity) {
            this.similarity = similarity;
            return this;
        }

    public SearchItem put(String key, Object value) {
            extra.put(key, value);
            return this;
        }
    }
