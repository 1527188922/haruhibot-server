package com.haruhi.botServer.vo;

import lombok.Data;

import java.util.List;

@Data
public class JmAlbumQueryReq extends PageReq {
    private Long id;
    private String name;
    private String author;
    private List<String> tags;
}
