package com.haruhi.botserver.infrastructure.persistence;

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
