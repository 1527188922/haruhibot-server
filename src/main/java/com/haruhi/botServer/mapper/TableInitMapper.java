package com.haruhi.botServer.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TableInitMapper {

    /**
     * 群聊天记录表
     * @param tableName
     * @return
     */
    int createGroupChatHistory(@Param("tableName") String tableName);
}
