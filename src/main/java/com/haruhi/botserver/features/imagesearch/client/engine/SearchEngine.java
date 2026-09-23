package com.haruhi.botserver.features.imagesearch.client.engine;

import com.haruhi.botserver.features.imagesearch.client.EngineType;
import com.haruhi.botserver.features.imagesearch.client.SearchInput;
import com.haruhi.botserver.features.imagesearch.client.SearchResponse;

public interface SearchEngine {
        EngineType type();

        SearchResponse search(SearchInput input);
    }
