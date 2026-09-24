package com.haruhi.botserver.features.jmcomic.model;

import lombok.Data;

/**
 * JM在线搜索(调用JM服务器/search接口)请求
 */
@Data
public class JmAlbumOnlineSearchReq {
    /**
     * 关键字，JM后端只会截取前8个字符参与搜索
     */
    private String name;
    /**
     * 排序，取值见 {@link JmSearchSortEnum#getSort()} ，为空或非法时按默认排序
     */
    private String sort;
    /**
     * 页码，从1开始
     */
    private Integer page;
}
