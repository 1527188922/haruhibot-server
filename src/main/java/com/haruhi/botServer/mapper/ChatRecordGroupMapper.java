package com.haruhi.botServer.mapper;

import com.haruhi.botServer.entity.ChatRecordGroup;
import com.haruhi.botServer.entity.vo.ChatRecordVo;
import com.haruhi.botServer.vo.ChatRecordQueryReq;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ChatRecordGroupMapper {

    int insert(@Param("tableName") String tableName, @Param("param") ChatRecordGroup entity);

    List<ChatRecordGroup> selectList(@Param("tableName") String tableName, @Param("param") ChatRecordQueryReq req);

    List<ChatRecordVo> chatStats(@Param("tableName") String tableName, @Param("selfId") Long selfId);

}
