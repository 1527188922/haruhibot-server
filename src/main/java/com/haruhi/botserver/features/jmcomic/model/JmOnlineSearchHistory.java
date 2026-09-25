package com.haruhi.botserver.features.jmcomic.model;

import lombok.Data;

/**
 * JM在线搜索历史记录
 * <p>
 * 持久化的是"这次搜了什么"，即搜索条件(关键字/排序/页码)与结果概要(总数/每页条数/实际返回条数)，
 * 而不是结果快照：点击历史记录时按原条件、原页码重新搜索一次即可，JM的数据本身是随时变化的。
 * <p>
 * id、sortLabel、searchTime 只在读取/下发时填充，不写入存储内容
 */
@Data
public class JmOnlineSearchHistory {
    /**
     * t_dictionary主键，仅用于删除
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
}
