package com.haruhi.botServer.mapper.sqlite;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.haruhi.botServer.entity.sqlite.ChatRecordSqlite;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatRecordSqliteMapper extends BaseMapper<ChatRecordSqlite> {
}
