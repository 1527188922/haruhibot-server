package com.haruhi.botserver.features.jmcomic.model;

import lombok.Data;

/**
 * JM收藏夹信息（含该收藏夹下的漫画数量）
 */
@Data
public class JmFavoriteResp {
    private Long id;
    private String name;
    /**
     * 展示排序，值越小越靠前
     */
    private Integer sortOrder;
    /**
     * 是否为默认收藏夹：默认收藏夹不允许删除或重命名
     */
    private Boolean isDefault;
    /**
     * 该收藏夹下的漫画数量
     */
    private Long albumCount;
    private String createTime;
    private String modifyTime;
}
