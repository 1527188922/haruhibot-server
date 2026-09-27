package com.haruhi.botserver.features.jmcomic.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.haruhi.botserver.features.jmcomic.persistence.entity.JmFavoriteSqlite;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface JmFavoriteSqliteMapper extends BaseMapper<JmFavoriteSqlite> {
}
