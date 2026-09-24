package com.haruhi.botserver.features.jmcomic.model;

import lombok.Data;

import java.util.List;

/**
 * JM主记录收藏/取消收藏请求
 */
@Data
public class JmAlbumCollectReq {
    private List<Long> ids;
    /**
     * true=收藏，其他=取消收藏
     */
    private Boolean collected;
}
