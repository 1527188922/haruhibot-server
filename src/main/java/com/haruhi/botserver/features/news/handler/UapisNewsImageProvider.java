package com.haruhi.botserver.features.news.handler;

import cn.hutool.core.net.url.UrlBuilder;
import cn.hutool.core.net.url.UrlQuery;
import com.haruhi.botserver.configuration.metadata.ConfigKey;
import com.haruhi.botserver.configuration.service.Configs;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * uapis每日新闻图片，接口直接返回图片
 */
@Component
public class UapisNewsImageProvider implements NewsImageProvider {

    @Override
    public String name() {
        return "uapis";
    }

    @Override
    public String imageUrl() {
        UrlBuilder builder = UrlBuilder.ofHttp(Configs.getStr(ConfigKey.NEWS_UAPIS_URL), StandardCharsets.UTF_8);
        Map<String, String> query = new LinkedHashMap<>();
        builder.getQuery().getQueryMap()
                .forEach((key, value) -> query.put(String.valueOf(key), String.valueOf(value)));
        query.put("t", String.valueOf(System.currentTimeMillis()));
        builder.setQuery(UrlQuery.of(query));
        return builder.build();
    }
}
