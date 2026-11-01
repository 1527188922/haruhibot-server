package com.haruhi.botserver.features.jmcomic.service;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 线路测速的纯逻辑部分：档位阈值、HTML 劫持识别、JM 信封识别。
 *
 * <p>阈值照官方 {@code speedTestUtil.ts}：绿 &lt;600ms、橙 600～1200ms、红 ≥1200ms。
 */
class JmLineProbeServiceTest {

    @Test
    void levelThresholds() {
        assertEquals("SMOOTH", JmLineProbeService.levelOf(0));
        assertEquals("SMOOTH", JmLineProbeService.levelOf(599));
        assertEquals("NORMAL", JmLineProbeService.levelOf(600));
        assertEquals("NORMAL", JmLineProbeService.levelOf(1199));
        assertEquals("LAGGY", JmLineProbeService.levelOf(1200));
        assertEquals("LAGGY", JmLineProbeService.levelOf(60_000));
        assertEquals(600L, JmLineProbeService.SMOOTH_UPPER_MS);
        assertEquals(1200L, JmLineProbeService.NORMAL_UPPER_MS);
    }

    @Test
    void detectsHijackedHtmlResponses() {
        byte[] html = "<!DOCTYPE html><html></html>".getBytes(StandardCharsets.UTF_8);
        assertTrue(JmLineProbeService.looksLikeHtml("text/html; charset=utf-8", html));
        // 没有 content-type 时看第一个有效字节
        assertTrue(JmLineProbeService.looksLikeHtml(null, html));
        assertTrue(JmLineProbeService.looksLikeHtml(null, "\n\t  <html>".getBytes(StandardCharsets.UTF_8)));

        // GIF/WebP 的真实首字节不可能是 '<'
        assertFalse(JmLineProbeService.looksLikeHtml("image/gif", "GIF89a".getBytes(StandardCharsets.UTF_8)));
        assertFalse(JmLineProbeService.looksLikeHtml("image/webp", "RIFFxxxxWEBP".getBytes(StandardCharsets.UTF_8)));
        assertFalse(JmLineProbeService.looksLikeHtml(null, new byte[0]));
    }

    @Test
    void detectsJmEnvelope() {
        assertTrue(JmLineProbeService.looksLikeJmEnvelope("{\"code\":200,\"data\":\"abc==\"}"));
        assertFalse(JmLineProbeService.looksLikeJmEnvelope("{\"code\":200,\"data\":\"\"}"));
        assertFalse(JmLineProbeService.looksLikeJmEnvelope("{\"code\":200}"));
        assertFalse(JmLineProbeService.looksLikeJmEnvelope("not json"));
        assertFalse(JmLineProbeService.looksLikeJmEnvelope(""));
        assertFalse(JmLineProbeService.looksLikeJmEnvelope(null));
    }
}
