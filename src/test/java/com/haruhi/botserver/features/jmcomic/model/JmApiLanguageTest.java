package com.haruhi.botserver.features.jmcomic.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * JM接口语言：默认简体中文，取值容错。
 *
 * <p>官方App的 {@code lang} 只有 {@code CN} / {@code TW} 两个值，
 * 认不出来的一律回落到简体中文，绝不因为脏数据发生"什么都对不上"。
 */
class JmApiLanguageTest {

    @Test
    void defaultIsSimplifiedChinese() {
        assertEquals(JmApiLanguage.CN, JmApiLanguage.DEFAULT);
        assertEquals("CN", JmApiLanguage.DEFAULT.getCode());
        assertEquals("简体中文", JmApiLanguage.DEFAULT.getRemark());
        assertEquals("TW", JmApiLanguage.TW.getCode());
    }

    @Test
    void parsesCodesIgnoringCaseAndBlank() {
        assertEquals(JmApiLanguage.CN, JmApiLanguage.getEnumByCode("CN"));
        assertEquals(JmApiLanguage.CN, JmApiLanguage.getEnumByCode(" cn "));
        assertEquals(JmApiLanguage.TW, JmApiLanguage.getEnumByCode("tw"));
        assertNull(JmApiLanguage.getEnumByCode("zh"));
        assertNull(JmApiLanguage.getEnumByCode(""));
        assertNull(JmApiLanguage.getEnumByCode(null));
    }

    @Test
    void unknownCodeFallsBackToSimplifiedChinese() {
        assertEquals(JmApiLanguage.CN, JmApiLanguage.getOrDefault(null));
        assertEquals(JmApiLanguage.CN, JmApiLanguage.getOrDefault("  "));
        assertEquals(JmApiLanguage.CN, JmApiLanguage.getOrDefault("ZH"));
        assertEquals(JmApiLanguage.CN, JmApiLanguage.getOrDefault("1"));
        assertEquals(JmApiLanguage.TW, JmApiLanguage.getOrDefault("TW"));
    }
}
