package com.haruhi.botServer.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SqlTypeEnum {
    QUERY,
    UPDATE,
    DDL,
    ERROR
}
