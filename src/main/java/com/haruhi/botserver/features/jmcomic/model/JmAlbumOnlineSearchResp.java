package com.haruhi.botserver.features.jmcomic.model;

import lombok.Data;

import java.util.List;

/**
 * JM在线搜索响应
 * 分页由JM服务器/search接口完成，每页固定80条，total上限10000(接口自身限制)
 */
@Data
public class JmAlbumOnlineSearchResp {
    /**
     * JM返回的搜索关键字(被截取后的)
     */
    private String searchQuery;
    /**
     * 总条数，JM接口上限10000
     */
    private Long total;
    private Integer page;
    /**
     * 每页条数，JM接口固定80
     */
    private Integer pageSize;
    private Integer totalPage;
    private List<Item> content;

    @Data
    public static class Item {
        private String id;
        private String name;
        private String author;
        /**
         * 分类，主分类/子分类拼接
         */
        private String category;
        /**
         * 封面图链接，搜索结果里JM只给了图片文件名，这里按jmId拼出封面地址
         */
        private String coverUrl;
        /**
         * 10位时间戳
         */
        private Long updateAt;
        private String updateTime;
        /**
         * 本地jm_album是否已存在该记录
         */
        private Boolean existsLocal;
    }
}
