package com.haruhi.botServer.mapper.sqlite;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.haruhi.botServer.entity.sqlite.ChatRecordSqlite;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ChatRecordSqliteMapper extends BaseMapper<ChatRecordSqlite> {

    /**
     * 分组统计群用户聊天记录
     * @param groupId
     * @return List<{ groupId,userId,total }>
     */
    List<ChatRecordSqlite> groupRecordCounting(@Param("groupId") Long groupId, @Param("selfId") Long selfId);
}
