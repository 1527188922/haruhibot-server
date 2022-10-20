package com.haruhi.botServer.constant.event;

public enum MessageEventEnum {

    group("group"),
    privat("private");

    private String type;

    MessageEventEnum(String type){
        this.type = type;
    }
    public String getType(){
        return type;
    }
}
