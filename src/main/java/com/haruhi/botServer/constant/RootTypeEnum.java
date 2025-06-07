package com.haruhi.botServer.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum RootTypeEnum {

    SYS_TOOT("1","系统根目录"),
    BOT_TOOT("2","BOT程序根目录"),
    ;


    private final String type;
    private final String label;
}
