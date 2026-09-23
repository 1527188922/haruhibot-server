package com.haruhi.botserver.infrastructure.persistence.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.haruhi.botserver.infrastructure.persistence.persistence.entity.SqliteSchema;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SqliteSchemaMapper extends BaseMapper<SqliteSchema> {
}
