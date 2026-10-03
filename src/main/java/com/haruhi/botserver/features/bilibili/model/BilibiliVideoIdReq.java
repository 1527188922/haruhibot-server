package com.haruhi.botserver.features.bilibili.model;

import lombok.Data;

import java.util.List;

/**
 * b站视频记录id请求参数
 */
@Data
public class BilibiliVideoIdReq {

    private Long id;

    /**
     * 多个记录id，批量操作时使用
     */
    private List<Long> ids;
}
