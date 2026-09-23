package com.haruhi.botserver.integration.onebot.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ElementTypeEnum {
    TEXT(1),
    PIC(2),
    REPLY(7),
    ;
    
    private int type;
}
