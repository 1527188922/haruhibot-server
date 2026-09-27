package com.haruhi.botserver.features.jmcomic.model;

import lombok.Data;

import java.util.List;

/**
 * 把漫画加入/移出收藏夹的请求。
 * <p>
 * 支持一次操作多个收藏夹（前端"收藏到收藏夹"可以多选），也兼容只操作一个：
 * <ul>
 *     <li>favoriteIds / favoriteNames：多选，二者可以同时传，按顺序合并</li>
 *     <li>favoriteId：单选，已存在的收藏夹</li>
 *     <li>favoriteName：单选，收藏夹名称；不存在时直接新建（名称会被 trim）</li>
 * </ul>
 * 单选与多选同时传时取并集；favoriteId 与 favoriteName 同时传时优先 favoriteId。
 * 一个都没传时回落到默认收藏夹（"更改收藏夹"保存空集合时除外，见 saveAlbumFavorites）。
 */
@Data
public class JmFavoriteAlbumReq {
    private List<Long> albumIds;
    private Long favoriteId;
    private String favoriteName;
    /**
     * 多选：收藏夹id列表
     */
    private List<Long> favoriteIds;
    /**
     * 多选：收藏夹名称列表，不存在时直接新建
     */
    private List<String> favoriteNames;
}
