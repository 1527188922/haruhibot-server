package com.haruhi.botserver.features.wordstrip.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.haruhi.botserver.features.wordstrip.persistence.entity.WordStripSqlite;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WordStripSqliteMapper extends BaseMapper<WordStripSqlite> {
}
