package com.haruhi.botserver.features.chatrecord.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.haruhi.botserver.features.chatrecord.persistence.entity.ChatRecordSqlite;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatRecordSqliteMapper extends BaseMapper<ChatRecordSqlite> {
}
