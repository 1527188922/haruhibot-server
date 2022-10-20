package com.haruhi.botServer.dto.searchImage.response;

import lombok.Data;

import java.io.Serializable;

@Data
public class Header implements Serializable {
    private String thumbnail;
    private int hidden;
    private int dupes;
    private Double similarity;
    private int index_id;
    private String index_name;
}
