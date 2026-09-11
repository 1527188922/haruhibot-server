package com.haruhi.botServer.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 订阅推送类型
 * 对应 t_subscribe.sub_type
 */
@AllArgsConstructor
@Getter
public enum BilibiliSubscribeTypeEnum {

    /**
     * b站主播开播/下播推送
     */
    LIVE("live", "b站主播开播推送"),
    ;

    private final String type;
    private final String remark;

    public static BilibiliSubscribeTypeEnum getEnumByType(String type) {
        for (BilibiliSubscribeTypeEnum value : BilibiliSubscribeTypeEnum.values()) {
            if (value.getType().equals(type)) {
                return value;
            }
        }
        return null;
    }
}
