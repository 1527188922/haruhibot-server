package com.haruhi.botServer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.haruhi.botServer.entity.SqliteSchema;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SqliteSchemaMapper extends BaseMapper<SqliteSchema> {
}
