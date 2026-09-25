package com.haruhi.botserver.features.jmcomic.model;

import lombok.Data;

import java.util.List;

/**
 * JM在线搜索历史记录
 * <p>
 * 持久化的是这次搜索的条件(关键字/排序/页码)、结果概要(总数/每页条数/实际返回条数)、
 * 以及那一页的搜索结果快照(items)，点击历史记录时直接回显快照，不再请求JM。
 * <p>
 * id、sortLabel、searchTime 只在读取/下发时填充，不写入存储内容
 */
@Data
public class JmOnlineSearchHistory {
    /**
     * t_dictionary主键，仅用于删除/查询详情
     */
    private Long id;
    /**
     * 搜索关键字
     */
    private String name;
    /**
     * 排序code，见 {@link JmSearchSortEnum#getSort()}
     */
    private String sort;
    /**
     * 排序中文说明，读取时由 {@link JmSearchSortEnum} 补全
     */
    private String sortLabel;
    private Integer page;
    /**
     * JM返回的结果总数
     */
    private Long total;
    /**
     * 每页条数
     */
    private Integer pageSize;
    /**
     * 本次实际返回的条数
     */
    private Integer resultCount;
    /**
     * 搜索时间(同条件重复搜索时会刷新)
     */
    private String searchTime;
    /**
     * 当时的搜索结果快照；
     * 历史列表接口不下发(置空)，只有查询单条详情时才返回
     */
    private List<JmAlbumOnlineSearchResp.Item> items;
}
