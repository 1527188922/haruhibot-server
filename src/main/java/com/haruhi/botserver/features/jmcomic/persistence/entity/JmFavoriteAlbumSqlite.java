package com.haruhi.botserver.features.jmcomic.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.haruhi.botserver.infrastructure.persistence.DataBaseConst;
import lombok.Data;

/**
 * JM收藏夹-漫画关联。
 * 多对多：同一本漫画可以同时存在于多个收藏夹；
 * (favorite_id, album_id) 上有唯一索引，保证同一收藏夹内不重复。
 */
@Data
@TableName(value = DataBaseConst.T_JM_FAVORITE_ALBUM)
public class JmFavoriteAlbumSqlite {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long favoriteId;
    private Long albumId;
    private String createTime;
}
