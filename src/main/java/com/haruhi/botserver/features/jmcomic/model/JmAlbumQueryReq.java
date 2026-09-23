package com.haruhi.botserver.features.jmcomic.model;

import com.haruhi.botserver.shared.model.PageReq;

import lombok.Data;

import java.util.List;

@Data
public class JmAlbumQueryReq extends PageReq {
    private Long id;
    private String name;
    private String author;
    private List<String> tags;
}
