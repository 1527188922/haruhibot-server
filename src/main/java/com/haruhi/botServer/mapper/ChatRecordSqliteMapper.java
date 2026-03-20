package com.haruhi.botServer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.haruhi.botServer.entity.ChatRecordSqlite;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatRecordSqliteMapper extends BaseMapper<ChatRecordSqlite> {
}
