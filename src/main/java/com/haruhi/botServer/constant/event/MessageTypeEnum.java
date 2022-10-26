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
}
