package com.haruhi.botServer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.haruhi.botServer.entity.sqlite.SendLikeRecordSqlite;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SendLikeRecordSqliteMapper extends BaseMapper<SendLikeRecordSqlite> {
}
