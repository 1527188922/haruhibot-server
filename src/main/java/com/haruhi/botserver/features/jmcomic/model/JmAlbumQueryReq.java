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
    /**
     * 本地收藏筛选：null=不限，true=只看已收藏，false=只看未收藏
     */
    private Boolean collected;
}
