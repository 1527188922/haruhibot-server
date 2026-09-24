package com.haruhi.botserver.features.jmcomic.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * JM /search 接口的排序方式
 * 对应请求参数 o
 */
@AllArgsConstructor
@Getter
public enum JmSearchSortEnum {

    /**
     * 最新
     */
    LATEST("mr", "最新"),
    /**
     * 最多观看
     */
    VIEW("mv", "最多观看"),
    /**
     * 最多图片
     */
    PICTURE("mp", "最多图片"),
    /**
     * 最多喜欢
     */
    LIKE("tf", "最多喜欢"),
    ;

    /**
     * 排序为空时的默认排序
     */
    public static final JmSearchSortEnum DEFAULT = LATEST;

    private final String sort;
    private final String remark;

    public static JmSearchSortEnum getEnumBySort(String sort) {
        for (JmSearchSortEnum value : JmSearchSortEnum.values()) {
            if (value.getSort().equals(sort)) {
                return value;
            }
        }
        return null;
    }

    /**
     * 按sort取枚举，取不到(含null)返回默认排序
     */
    public static JmSearchSortEnum getOrDefault(String sort) {
        JmSearchSortEnum value = getEnumBySort(sort);
        return value == null ? DEFAULT : value;
    }
}
