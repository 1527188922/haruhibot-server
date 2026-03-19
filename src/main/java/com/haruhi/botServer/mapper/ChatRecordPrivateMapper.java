package com.haruhi.botServer.mapper;

import com.haruhi.botServer.entity.ChatRecordPrivate;
import com.haruhi.botServer.vo.ChatRecordQueryReq;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ChatRecordPrivateMapper {

    int insert(@Param("tableName") String tableName, @Param("param") ChatRecordPrivate entity);

    List<ChatRecordPrivate> selectList(@Param("tableName") String tableName, @Param("param") ChatRecordQueryReq req);
}
