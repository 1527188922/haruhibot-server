package com.haruhi.botServer.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 订阅推送目标类型
 */
@AllArgsConstructor
@Getter
public enum PushTargetTypeEnum {

    /**
     * 群
     */
    GROUP("group", "群"),
    /**
     * 好友
     */
    FRIEND("friend", "好友"),
    ;

    private final String type;
    private final String remark;

    public static PushTargetTypeEnum getEnumByType(String type) {
        for (PushTargetTypeEnum value : PushTargetTypeEnum.values()) {
            if (value.getType().equals(type)) {
                return value;
            }
        }
        return null;
    }
}
