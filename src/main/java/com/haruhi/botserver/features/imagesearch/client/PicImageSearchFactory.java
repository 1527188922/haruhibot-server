package com.haruhi.botserver.features.imagesearch.client;

import com.haruhi.botserver.features.imagesearch.client.engine.AnimeTrace;
import com.haruhi.botserver.features.imagesearch.client.engine.Ascii2D;
import com.haruhi.botserver.features.imagesearch.client.engine.Baidu;
import com.haruhi.botserver.features.imagesearch.client.engine.Bing;
import com.haruhi.botserver.features.imagesearch.client.engine.Copyseeker;
import com.haruhi.botserver.features.imagesearch.client.engine.EHentai;
import com.haruhi.botserver.features.imagesearch.client.engine.Google;
import com.haruhi.botserver.features.imagesearch.client.engine.GoogleLens;
import com.haruhi.botserver.features.imagesearch.client.engine.Iqdb;
import com.haruhi.botserver.features.imagesearch.client.engine.Lenso;
import com.haruhi.botserver.features.imagesearch.client.engine.SauceNao;
import com.haruhi.botserver.features.imagesearch.client.engine.SearchEngine;
import com.haruhi.botserver.features.imagesearch.client.engine.Tineye;
import com.haruhi.botserver.features.imagesearch.client.engine.TraceMoe;
import com.haruhi.botserver.features.imagesearch.client.engine.Yandex;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class PicImageSearchFactory {

    private final Map<EngineType, SearchEngine> engineMap = new EnumMap<>(EngineType.class);

    public PicImageSearchFactory(List<SearchEngine> engines) {
        for (SearchEngine engine : engines) {
            SearchEngine oldEngine = engineMap.put(engine.type(), engine);
            if (oldEngine != null) {
                throw new IllegalStateException("Duplicate image search engine: " + engine.type());
            }
        }
    }

    public SearchEngine getEngine(EngineType type) {
        SearchEngine engine = engineMap.get(type);
        if (engine == null) {
            throw new IllegalArgumentException("Image search engine not found: " + type);
        }
        return engine;
    }

    public List<SearchEngine> getAllEngines() {
        return engineMap.values().stream().toList();
    }

    public SauceNao sauceNao() {
        return (SauceNao) getEngine(EngineType.SAUCENAO);
    }

    public TraceMoe traceMoe() {
        return (TraceMoe) getEngine(EngineType.TRACE_MOE);
    }

    public AnimeTrace animeTrace() {
        return (AnimeTrace) getEngine(EngineType.ANIME_TRACE);
    }

    public Ascii2D ascii2D() {
        return (Ascii2D) getEngine(EngineType.ASCII2D);
    }

    public Ascii2D ascii2DBovw() {
        return (Ascii2D) getEngine(EngineType.ASCII2D_BOVW);
    }

    public Iqdb iqdb() {
        return (Iqdb) getEngine(EngineType.IQDB);
    }

    public Iqdb iqdb3D() {
        return (Iqdb) getEngine(EngineType.IQDB_3D);
    }

    public Baidu baidu() {
        return (Baidu) getEngine(EngineType.BAIDU);
    }

    public Bing bing() {
        return (Bing) getEngine(EngineType.BING);
    }

    public Tineye tineye() {
        return (Tineye) getEngine(EngineType.TINEYE);
    }

    public Google google() {
        return (Google) getEngine(EngineType.GOOGLE);
    }

    public GoogleLens googleLens() {
        return (GoogleLens) getEngine(EngineType.GOOGLE_LENS);
    }

    public Yandex yandex() {
        return (Yandex) getEngine(EngineType.YANDEX);
    }

    public Copyseeker copyseeker() {
        return (Copyseeker) getEngine(EngineType.COPYSEEKER);
    }

    public Lenso lenso() {
        return (Lenso) getEngine(EngineType.LENSO);
    }

    public EHentai eHentai() {
        return (EHentai) getEngine(EngineType.EHENTAI);
    }

    public EHentai exHentai() {
        return (EHentai) getEngine(EngineType.EXHENTAI);
    }
}
