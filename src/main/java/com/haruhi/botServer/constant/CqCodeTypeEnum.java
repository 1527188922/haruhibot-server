package com.haruhi.botServer.constant;

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
}
