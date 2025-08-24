package com.haruhi.botServer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.haruhi.botServer.constant.DataBaseConst;
import lombok.Data;

@Data
@TableName(value = DataBaseConst.T_PIXIV)
public class PixivSqlite {

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
    private Integer isR18;
    private String tags;
}
