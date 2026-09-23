package com.haruhi.botserver.administration.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.haruhi.botserver.administration.persistence.entity.SystemLogSqlite;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SystemLogSqliteMapper extends BaseMapper<SystemLogSqlite> {
}
