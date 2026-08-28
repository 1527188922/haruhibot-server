package com.haruhi.botServer.vo;

import lombok.Data;

@Data
public class JmAlbumQueryReq extends PageReq {
    private Long id;
    private String name;
    private String author;
    private String tags;
}
