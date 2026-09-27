package com.haruhi.botserver.features.jmcomic.model;

import lombok.Data;

import java.util.List;

/**
 * 把漫画加入/移出收藏夹的请求。
 * <p>
 * 兼容两种定位收藏夹的方式，二者取其一：
 * <ul>
 *     <li>favoriteId：已存在的收藏夹</li>
 *     <li>favoriteName：收藏夹名称；不存在时直接新建（名称会被 trim），与 favoriteId 同时传时优先 favoriteId</li>
 * </ul>
 * 都不传时回落到默认收藏夹。
 */
@Data
public class JmFavoriteAlbumReq {
    private List<Long> albumIds;
    private Long favoriteId;
    private String favoriteName;
}
