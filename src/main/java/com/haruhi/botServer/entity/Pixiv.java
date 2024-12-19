package com.haruhi.botServer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.haruhi.botServer.config.DataBaseConfig;
import lombok.Data;

@Data
@TableName(value = DataBaseConfig.T_PIXIV)
public class Pixiv {
    @TableId(value = "id",type = IdType.AUTO)
    private Long id;
    private String pid;
    private String title;
    private Integer width;
    private Integer height;
    @TableField("`view`")
    private Integer view;
    private Integer bookmarks;
    private String imgUrl;
    private String imgP;
    private String uid;
    private String author;
    private Boolean isR18;
    private String tags;
}
