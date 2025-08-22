package com.haruhi.botServer.mapper;

import com.haruhi.botServer.entity.TableInfoSqlite;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SqliteDatabaseInitMapper {

    List<TableInfoSqlite> pragmaTableInfo(@Param("tableName") String tableName);



    int createIndex(@Param("tableName") String tableName, @Param("column") String column);

    int addColumn(@Param("tableName") String tableName, @Param("column") String column,@Param("type") String type,@Param("notNull") boolean notNull,@Param("default") String defaultValue);

    /**
     * 群聊天记录表
     * @param tableName
     * @return
     */
    int createChatRecord(@Param("tableName") String tableName);


    int createChatRecordExtend(@Param("tableName") String tableName);

    /**
     * 戳一戳回复表
     * @param tableName
     * @return
     */
    int createPokeReply(@Param("tableName") String tableName);

    /**
     * 自定义回复表
     * @param tableName
     * @return
     */
    int createCustomReply(@Param("tableName") String tableName);

    /**
     * 词条表
     * @param tableName
     * @return
     */
    int createWordStrip(@Param("tableName") String tableName);
    /**
     * pixiv图库表
     * @param tableName
     * @return
     */
    int createPixiv(@Param("tableName") String tableName);


    int createSendLikeRecord(@Param("tableName") String tableName);

    int createDictionary(@Param("tableName") String tableName);

    int createGroupInfo(@Param("tableName") String tableName);
}
