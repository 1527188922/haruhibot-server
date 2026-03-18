package com.haruhi.botServer.mapper;

import com.haruhi.botServer.entity.ChatRecordPrivate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ChatRecordPrivateMapper {

    int insert(@Param("tableName") String tableName, @Param("param") ChatRecordPrivate entity);
}
