package com.haruhi.botserver.features.jmcomic.util;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JM 线路清单测试：默认项在最前、清单去重、下拉候选项格式、域名规整。
 *
 * <p>清单同时是「配置管理」下拉与测速的数据源，格式写错会让配置页保存成错误的值。
 */
class JmLineCatalogTest {

    @Test
    void defaultLinesComeFirst() {
        assertEquals("www.cdnbea.net", JmLineCatalog.apiLines().get(0).host());
        assertTrue(JmLineCatalog.apiLines().get(0).builtIn());

        assertEquals("cdn-msp2.jmapiproxy2.cc", JmLineCatalog.imageLines().get(0).host());
        assertTrue(JmLineCatalog.imageLines().get(0).builtIn());

        assertEquals(JmLineCatalog.DEFAULT_API_HOST, JmLineCatalog.apiLines().get(0).host());
        assertEquals(JmLineCatalog.DEFAULT_IMAGE_HOST, JmLineCatalog.imageLines().get(0).host());
    }

    @Test
    void linesAreUniqueAndCoverTheAndroidCatalog() {
        // 官方线路清单 5 条 + 默认线路 = 6 条
        assertEquals(6, JmLineCatalog.apiLines().size());
        // 12 条实测图源 + 默认线路 = 13 条
        assertEquals(13, JmLineCatalog.imageLines().size());

        assertUnique(JmLineCatalog.apiLines());
        assertUnique(JmLineCatalog.imageLines());

        // 抽查几个固定值，防止以后改清单时不小心换掉默认线路
        assertTrue(hosts(JmLineCatalog.apiLines()).containsAll(List.of(
                "www.cdnhjk.net", "www.cdngwc.cc", "www.cdngwc.net", "www.cdngwc.club", "www.cdnutc.me")));
        assertTrue(hosts(JmLineCatalog.imageLines()).containsAll(List.of(
                "cdn-msp.jmapiproxy1.cc", "cdn-msp2.jmapiproxy3.cc", "tencent.jmdanjonproxy.xyz")));
    }

    /**
     * 下拉候选项必须是 {@code 值:显示名} 逗号分隔的格式，且显示名里不能再出现逗号
     * （ControlMeta 按逗号切分），否则候选项会被切坏。
     */
    @Test
    void controlOptionsAreCommaSafe() {
        assertOptionFormat(JmLineCatalog.apiControlOptions(), JmLineCatalog.apiLines());
        assertOptionFormat(JmLineCatalog.imageControlOptions(), JmLineCatalog.imageLines());
    }

    @Test
    void normalizeHostStripsSchemePathAndCasing() {
        assertEquals("www.cdngwc.cc", JmLineCatalog.normalizeHost("  https://WWW.Cdngwc.cc/album/1?x=2#y "));
        assertEquals("www.cdngwc.cc", JmLineCatalog.normalizeHost("www.cdngwc.cc"));
        assertEquals("", JmLineCatalog.normalizeHost(null));
        assertEquals("", JmLineCatalog.normalizeHost("   "));
        assertEquals("cdn-msp2.jmapiproxy2.cc",
                JmLineCatalog.normalizeHost("cdn-msp2.jmapiproxy2.cc/media/photos/1/00001.webp"));
    }

    @Test
    void lineLookupFallsBackToCustomHost() {
        assertEquals(JmLineCatalog.DEFAULT_API_HOST, JmLineCatalog.apiLineOf(null).host());
        assertEquals(JmLineCatalog.DEFAULT_API_HOST, JmLineCatalog.apiLineOf("").host());
        assertEquals("线路 2", JmLineCatalog.apiLineOf("https://www.cdngwc.cc/x").label());

        JmLineCatalog.Line custom = JmLineCatalog.imageLineOf("my.own.host");
        assertEquals("my.own.host", custom.host());
        assertFalse(custom.builtIn(), "清单外的域名必须标记为非内置，配置页仍要能显示它");
        assertTrue(JmLineCatalog.isDefaultImage("https://cdn-msp2.jmapiproxy2.cc/a.jpg"));
        assertFalse(JmLineCatalog.isDefaultApi("www.cdngwc.cc"));
    }

    @Test
    void kindCodesAreStable() {
        assertEquals("JM_API", JmLineCatalog.Kind.API.code());
        assertEquals("JM_IMAGE", JmLineCatalog.Kind.IMAGE.code());
        assertEquals(JmLineCatalog.Kind.API, JmLineCatalog.Kind.fromCode("jm_api"));
        assertEquals(JmLineCatalog.Kind.IMAGE, JmLineCatalog.Kind.fromCode(" JM_IMAGE "));
        assertEquals(JmLineCatalog.Kind.API, JmLineCatalog.Kind.fromCode("API"));
        assertNull(JmLineCatalog.Kind.fromCode("nope"));
        assertNull(JmLineCatalog.Kind.fromCode(null));
        assertEquals(6, JmLineCatalog.Kind.API.lines().size());
        assertEquals(13, JmLineCatalog.Kind.IMAGE.lines().size());
    }

    private static void assertOptionFormat(String raw, List<JmLineCatalog.Line> lines) {
        assertNotNull(raw);
        String[] items = raw.split(",");
        assertEquals(lines.size(), items.length, "候选项数量必须与清单一致");
        for (int i = 0; i < items.length; i++) {
            String item = items[i];
            int sep = item.indexOf(':');
            assertTrue(sep > 0, "候选项必须带冒号分隔： " + item);
            assertEquals(lines.get(i).host(), item.substring(0, sep).trim(), "冒号前必须是域名");
            String label = item.substring(sep + 1).trim();
            assertFalse(label.contains(","), "显示名不能再有逗号： " + label);
            assertTrue(label.contains(lines.get(i).host()), "显示名里要带域名，用户只看下拉也能分辨");
        }
    }

    private static Set<String> hosts(List<JmLineCatalog.Line> lines) {
        Set<String> set = new LinkedHashSet<>();
        lines.forEach(line -> set.add(line.host()));
        return set;
    }

    private static void assertUnique(List<JmLineCatalog.Line> lines) {
        Set<String> seen = new LinkedHashSet<>();
        for (JmLineCatalog.Line line : lines) {
            assertTrue(seen.add(line.host()), "清单里重复的线路：" + line.host());
        }
    }
}
