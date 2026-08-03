package com.haruhi.botServer.picimagesearch;

import lombok.Getter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Getter
public class SearchResponse {
        private final EngineType engine;
        private final String url;
        private final Object origin;
        private final int statusCode;
        private final List<SearchItem> raw = new ArrayList<>();
        private final Map<String, Object> extra = new LinkedHashMap<>();

        public SearchResponse(EngineType engine, String url, Object origin, int statusCode) {
            this.engine = engine;
            this.url = url;
            this.origin = origin;
            this.statusCode = statusCode;
        }

    public SearchResponse add(SearchItem item) {
            if (item != null) {
                raw.add(item);
            }
            return this;
        }

        public SearchResponse put(String key, Object value) {
            extra.put(key, value);
            return this;
        }
    }
