package com.haruhi.botServer.constant;

import org.apache.commons.lang3.StringUtils;

public enum CqCodeTypeEnum {
    at("at"),
    image("image"),
    face("face"),
    forward("forward"),
    reply("reply"),
    tts("tts"),
    poke("poke"),
    record("record"),
    music("music");


    private String type;
    CqCodeTypeEnum(String type){
        this.type = type;
    }
    public String getType(){
        return type;
    }
    
    public static CqCodeTypeEnum getByType(String type){
        if(StringUtils.isBlank(type)){
            return null;
        }

        for (CqCodeTypeEnum value : values()) {
            if (value.type.equals(type)) {
                return value;
            }
        }
        
        return null;
    }
}
