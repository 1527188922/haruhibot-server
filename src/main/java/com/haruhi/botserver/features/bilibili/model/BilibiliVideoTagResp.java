package com.haruhi.botserver.features.bilibili.model;

import lombok.Data;

/**
 * bilibili视频标签分组项（webui左侧分组列表）
 */
@Data
public class BilibiliVideoTagResp {

    private String tag;

    /**
     * 带该标签的视频数量
     */
    private Long videoCount;
}
