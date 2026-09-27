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
     * true=收藏，其他=取消收藏。
     * 取消收藏会同时把漫画从所有收藏夹移出。
     */
    private Boolean collected;
    /**
     * 收藏到指定收藏夹id，可为空；为空时回落到默认收藏夹
     */
    private Long favoriteId;
    /**
     * 收藏到指定收藏夹名称，可为空。
     * 名称不存在时直接新建该收藏夹再收藏；
     * 与 favoriteId 同时传时优先 favoriteId。仅在 collected=true 时生效。
     */
    private String favoriteName;
}
