package com.haruhi.botserver.features.bilibili.model;

import lombok.Data;

/**
 * bilibili视频作者分组项（webui左侧分组列表）
 */
@Data
public class BilibiliVideoAuthorResp {

    private Long ownerMid;

    private String ownerName;

    private String ownerFace;

    /**
     * 该作者入库的视频数量
     */
    private Long videoCount;
}
