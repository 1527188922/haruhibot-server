package com.haruhi.botServer.constant.event;

public enum MessageTypeEnum {

    group("group"),
    privat("private");

    private String type;

    MessageTypeEnum(String type){
        this.type = type;
    }
    public String getType(){
        return type;
    }
    
    
    public static MessageTypeEnum getEnumByType(String type){
        for (MessageTypeEnum value : MessageTypeEnum.values()) {
            if(value.getType().equals(type)){
                return value;
            }
        }
        return null;
    }
}
