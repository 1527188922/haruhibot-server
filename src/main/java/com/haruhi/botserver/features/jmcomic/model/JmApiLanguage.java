package com.haruhi.botserver.features.jmcomic.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * JM接口语言，对应请求参数 {@code lang}（官方App的 {@code localStorage.lang}）。
 * <p>
 * 官方App的 {@code HttpUtil.fetchGet} 会<b>给每一个GET请求</b>自动补一个 {@code lang}：
 * <pre>{@code
 * if (!searchParams.has("lang")) {
 *     searchParams.set("lang", localStorage.getItem("lang") || "TW");
 * }
 * }</pre>
 * 服务端据此返回中文文案的对应形态，例如分类名简中是 {@code 汉化}、繁中是 {@code 漢化}，
 * 本子详情(/album)与搜索结果(/search)里的标签、简介也是同样的规则。
 * <p>
 * 取值只有两个：{@code "CN"} 简中、{@code "TW"} 繁中。本工程默认走 {@link #DEFAULT}（简体中文），
 * 需要繁体时由调用方显式传入 {@link #TW}；不传/传了认不出的值时一律回落到简体中文。
 */
@AllArgsConstructor
@Getter
public enum JmApiLanguage {

    /**
     * 简体中文
     */
    CN("CN", "简体中文"),
    /**
     * 繁体中文
     */
    TW("TW", "繁体中文"),
    ;

    /**
     * 未指定语言时的默认值：简体中文
     */
    public static final JmApiLanguage DEFAULT = CN;

    private final String code;
    private final String remark;

    /**
     * 按code取枚举（忽略大小写与首尾空白），取不到返回null
     */
    public static JmApiLanguage getEnumByCode(String code) {
        if (code == null) {
            return null;
        }
        String value = code.trim();
        for (JmApiLanguage language : values()) {
            if (language.getCode().equalsIgnoreCase(value)) {
                return language;
            }
        }
        return null;
    }

    /**
     * 按code取枚举，取不到(含null、空串、非法值)返回默认的简体中文
     */
    public static JmApiLanguage getOrDefault(String code) {
        JmApiLanguage language = getEnumByCode(code);
        return language == null ? DEFAULT : language;
    }
}
