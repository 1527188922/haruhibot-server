package com.haruhi.botServer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.haruhi.botServer.constant.DataBaseConst;
import lombok.Data;

@Data
@TableName(value = DataBaseConst.T_JM_CHAPTER_IMAGE)
public class JmChapterImageSqlite {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long albumId;
    private Long chapterId;
    private String chapterSort;
    private String chapterTitle;
    private String chapterName;
    private String chapterAddTime;
    private String seriesId;
    private Boolean liked;
    private Boolean isFavorite;
    private String imageFile;
    private Integer imageSort;
}
