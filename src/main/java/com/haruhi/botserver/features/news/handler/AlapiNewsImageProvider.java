package com.haruhi.botserver.features.news.handler;

import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSONObject;
import com.haruhi.botserver.configuration.metadata.ConfigKey;
import com.haruhi.botserver.configuration.service.Configs;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * alapi早报，接口返回JSON，取data.image字段
 */
@Component
public class AlapiNewsImageProvider implements NewsImageProvider {

    private static final int TIMEOUT_MILLIS = 10 * 1000;

    @Override
    public String name() {
        return "alapi";
    }

    @Override
    public String imageUrl() {
        Map<String, Object> params = new HashMap<>();
        params.put("token", Configs.getStr(ConfigKey.NEWS_ALAPI_TOKEN));
        params.put("format", "json");
        String url = HttpUtil.urlWithForm(Configs.getStr(ConfigKey.NEWS_ALAPI_URL), params, StandardCharsets.UTF_8, false);

        try (HttpResponse response = HttpUtil.createGet(url).timeout(TIMEOUT_MILLIS).execute()) {
            JSONObject json = JSONObject.parseObject(response.body());
            if (json == null || !json.getBooleanValue("success")) {
                throw new IllegalStateException("alapi返回失败：" + (json == null ? "响应为空" : json.getString("message")));
            }
            JSONObject data = json.getJSONObject("data");
            String image = data == null ? null : data.getString("image");
            if (StringUtils.isBlank(image)) {
                throw new IllegalStateException("alapi未返回image字段");
            }
            return image;
        }
    }
}
