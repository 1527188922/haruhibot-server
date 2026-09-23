package com.haruhi.botserver.integration.onebot.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum MessageTypeEnum {

    group("group"),
    privat("private");

    private String type;


    public static MessageTypeEnum getEnumByType(String type){
        for (MessageTypeEnum value : MessageTypeEnum.values()) {
            if(value.getType().equals(type)){
                return value;
            }
        }
        return null;
    }
}
