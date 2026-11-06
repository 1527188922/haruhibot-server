package com.haruhi.botserver.features.jmcomic.service;

import cn.hutool.http.HttpUtil;
import com.haruhi.botserver.features.jmcomic.model.JmApiLanguage;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JM接口请求必须带语言参数，且默认是简体中文。
 *
 * <p>对齐 Android 工程 {@code core/protocol/ApiScheme.kt}：官方App给每个GET请求自动补
 * {@code lang}，服务端据此返回简中 / 繁中的文案。不传语言时本工程统一走简体中文，
 * 否则服务端默认回的是繁体（{@code 漢化}）。
 */
class JmcomicLanguageTest {

    @Test
    void languageDefaultsToSimplifiedChinese() {
        Map<String, Object> params = JmcomicService.withLanguage(new HashMap<>(), null);
        assertEquals("CN", params.get("lang"));
        assertEquals("CN", JmcomicService.withLanguage(new HashMap<>(), JmApiLanguage.CN).get("lang"));
    }

    @Test
    void explicitTraditionalChineseWins() {
        Map<String, Object> params = JmcomicService.withLanguage(new HashMap<>(), JmApiLanguage.TW);
        assertEquals("TW", params.get("lang"));
    }

    @Test
    void requestUrlCarriesLanguage() {
        Map<String, Object> params = new HashMap<>();
        params.put("id", "452699");
        JmcomicService.withLanguage(params, JmApiLanguage.DEFAULT);
        // 与 JmcomicService 内部拼URL的方式一致（Hutool urlWithForm，空格编码为%20）
        String url = HttpUtil.urlWithForm("https://example.com/album", params, StandardCharsets.UTF_8, false);
        assertTrue(url.contains("lang=CN"), url);
        assertTrue(url.contains("id=452699"), url);
    }
}
