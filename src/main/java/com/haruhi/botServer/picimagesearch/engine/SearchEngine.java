package com.haruhi.botServer.picimagesearch.engine;


import com.haruhi.botServer.picimagesearch.EngineType;
import com.haruhi.botServer.picimagesearch.SearchInput;
import com.haruhi.botServer.picimagesearch.SearchResponse;

public interface SearchEngine {
        EngineType type();

        SearchResponse search(SearchInput input);
    }
