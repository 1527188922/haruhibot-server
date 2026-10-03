package com.haruhi.botserver.features.bilibili.model;

import com.haruhi.botserver.shared.model.PageReq;
import lombok.Data;

/**
 * bilibili视频查询条件（webui管理页）
 */
@Data
public class BilibiliVideoQueryReq extends PageReq {

    /**
     * bv号，模糊查询
     */
    private String bvid;

    /**
     * 标题，模糊查询
     */
    private String title;

    /**
     * up主uid
     */
    private Long ownerMid;

    /**
     * up主昵称，模糊查询
     */
    private String ownerName;

    /**
     * 标签，模糊查询
     */
    private String tag;
}
