package com.haruhi.botserver.features.jmcomic.model;

import lombok.Data;

import java.util.List;

/**
 * 把漫画从收藏夹移出的请求。
 * favoriteId 为空时表示从"所有收藏夹"移出（等价于取消收藏）。
 */
@Data
public class JmFavoriteAlbumRemoveReq {
    private Long favoriteId;
    private List<Long> albumIds;
}
