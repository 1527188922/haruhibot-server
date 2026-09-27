package com.haruhi.botserver.features.jmcomic.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.haruhi.botserver.infrastructure.persistence.DataBaseConst;
import lombok.Data;

/**
 * JM收藏夹（文件夹）。
 * 名称唯一，默认收藏夹由初始化流程写入且不允许删除/重命名。
 */
@Data
@TableName(value = DataBaseConst.T_JM_FAVORITE)
public class JmFavoriteSqlite {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private String name;
    /**
     * 展示排序，值越小越靠前
     */
    private Integer sortOrder;
    private String createTime;
    private String modifyTime;
}
