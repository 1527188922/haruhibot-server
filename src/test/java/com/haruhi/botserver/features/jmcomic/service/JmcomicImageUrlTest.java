package com.haruhi.botserver.features.jmcomic.service;

import com.haruhi.botserver.features.jmcomic.util.JmLineCatalog;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 图片 / 封面 URL 必须走「配置里的图片域名」。
 *
 * <p>{@code jm.image_domain} 是「配置管理」页可改、可测速的项；如果拼接时还写死常量，
 * 用户换了图源也不会生效。
 */
class JmcomicImageUrlTest {

    @Test
    void imageUrlUsesConfiguredImageDomain() {
        String url = JmcomicService.buildImgUrl(749671L, "00001.webp");
        assertEquals("https://" + JmcomicService.getJmImageDomain() + "/media/photos/749671/00001.webp", url);
        assertTrue(url.startsWith("https://"), "章节图必须是 https");
    }

    @Test
    void coverUrlFallsBackToConfiguredImageDomain() {
        assertEquals("https://" + JmcomicService.getJmImageDomain() + "/media/albums/452699.jpg",
                JmcomicService.buildCoverUrl(452699L, null));
        // 指定域名时优先用指定的（封面下载重试会指定备用线路）
        assertEquals("https://cdn-msp3.18comic.vip/media/albums/452699.jpg",
                JmcomicService.buildCoverUrl(452699L, "cdn-msp3.18comic.vip"));
    }

    @Test
    void defaultImageDomainIsInTheCatalog() {
        assertEquals("cdn-msp2.jmapiproxy2.cc", JmcomicService.IMAGE_DOMAIN);
        assertEquals(JmLineCatalog.DEFAULT_IMAGE_HOST, JmcomicService.IMAGE_DOMAIN);
        assertTrue(JmLineCatalog.imageLines().stream()
                .anyMatch(line -> line.host().equals(JmcomicService.IMAGE_DOMAIN)));
    }
}
