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

    /**
     * 戳一戳回复表
     * @param tableName
     * @return
     */
    int createPokeReply(@Param("tableName") String tableName);

    /**
     * 话术表
     * @param tableName
     * @return
     */
    int createVerbalTricks(@Param("tableName") String tableName);
}
