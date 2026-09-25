package com.haruhi.botserver.features.jmcomic.model;

import lombok.Data;

import java.util.List;

/**
 * 删除JM在线搜索历史
 */
@Data
public class JmOnlineSearchHistoryDeleteReq {
    /**
     * 要删除的记录id
     */
    private List<Long> ids;
    /**
     * true=清空全部历史，此时忽略ids
     */
    private Boolean clearAll;
}
