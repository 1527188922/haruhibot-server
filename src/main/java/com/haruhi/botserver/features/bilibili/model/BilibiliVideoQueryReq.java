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
     * 标签，模糊查询（查询框用）
     */
    private String tag;

    /**
     * 标签，精确匹配（左侧标签分组列表点击时用）。
     * 标签是逗号分隔保存的，精确匹配指视频的标签里含有这一个完整标签
     */
    private String tagExact;
}
