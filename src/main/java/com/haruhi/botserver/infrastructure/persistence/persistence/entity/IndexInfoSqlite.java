package com.haruhi.botserver.infrastructure.persistence.persistence.entity;

import lombok.Data;

@Data
public class IndexInfoSqlite {

    private Integer seq;
    private String name;
    private Integer unique;
    private String origin;
    private Integer partial;
}
