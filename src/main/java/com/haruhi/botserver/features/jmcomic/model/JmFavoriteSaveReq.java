package com.haruhi.botserver.features.jmcomic.model;

import lombok.Data;

/**
 * JM收藏夹新增/重命名请求
 */
@Data
public class JmFavoriteSaveReq {
    /**
     * 收藏夹id，重命名时必填；新增时忽略
     */
    private Long id;
    /**
     * 收藏夹名称
     */
    private String name;
}