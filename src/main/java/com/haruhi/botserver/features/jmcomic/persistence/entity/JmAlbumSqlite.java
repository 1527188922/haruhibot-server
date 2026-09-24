package com.haruhi.botserver.features.jmcomic.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.haruhi.botserver.infrastructure.persistence.DataBaseConst;
import lombok.Data;

@Data
@TableName(value = DataBaseConst.T_JM_ALBUM)
public class JmAlbumSqlite {

    @TableId(value = "id", type = IdType.INPUT)
    private Long id;
    private String name;
    private String albumFolderName;
    private String images;
    private String addTime;
    private String description;
    private String totalViews;
    private String likes;
    private String series;
    private String seriesId;
    private String commentTotal;
    private String author;
    private String tags;
    private String works;
    private String actors;
    private String relatedList;
    private Boolean liked;
    private Boolean isFavorite;
    /**
     * 本地收藏标记(仅存本库，重新拉取JM数据不会覆盖)
     * 注意：不要给这个字段设默认值，否则 updateById 会把已有记录的收藏状态重置
     */
    private Boolean collected;
    private Boolean isAids;
    private String price;
    private String purchased;
    private String raw;
    private String createTime;
    private String modifyTime;
}
