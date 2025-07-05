package com.haruhi.botServer.mapper.sqlite;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.haruhi.botServer.entity.sqlite.PixivSqlite;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PixivSqliteMapper extends BaseMapper<PixivSqlite> {
}
