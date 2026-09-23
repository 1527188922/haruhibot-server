package com.haruhi.botserver.features.chatrecord.persistence.mapper;

import com.haruhi.botserver.features.chatrecord.persistence.entity.ChatRecordPrivate;
import com.haruhi.botserver.features.chatrecord.model.ChatRecordQueryReq;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ChatRecordPrivateMapper {

    int insert(@Param("tableName") String tableName, @Param("param") ChatRecordPrivate entity);

    List<ChatRecordPrivate> selectList(@Param("tableName") String tableName, @Param("param") ChatRecordQueryReq req);

    ChatRecordPrivate selectById(@Param("tableName") String tableName, @Param("id") long id);


    List<ChatRecordPrivate> selectListByTime(@Param("tableName") String tableName,
                                             @Param("targetId") long targetId,
                                             @Param("before") boolean before,
                                             @Param("time") String time,
                                             @Param("offset") long offset);
}
