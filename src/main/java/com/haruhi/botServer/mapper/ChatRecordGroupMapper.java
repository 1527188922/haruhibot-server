package com.haruhi.botServer.mapper;

import com.haruhi.botServer.entity.ChatRecordGroup;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ChatRecordGroupMapper {

    int insert(@Param("tableName") String tableName, @Param("param") ChatRecordGroup entity);

}
