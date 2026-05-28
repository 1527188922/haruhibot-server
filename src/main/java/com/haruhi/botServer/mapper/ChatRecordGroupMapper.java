package com.haruhi.botServer.mapper;

import com.haruhi.botServer.entity.ChatRecordGroup;
import com.haruhi.botServer.entity.ChatRecordPrivate;
import com.haruhi.botServer.entity.vo.ChatRecordVo;
import com.haruhi.botServer.vo.ChatRecordQueryReq;
import com.haruhi.botServer.vo.CodeNameResp;
import com.haruhi.botServer.vo.GroupChatUserResp;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ChatRecordGroupMapper {

    int insert(@Param("tableName") String tableName, @Param("param") ChatRecordGroup entity);

    List<ChatRecordGroup> selectList(@Param("tableName") String tableName, @Param("param") ChatRecordQueryReq req);

    List<ChatRecordGroup> selectByIds(@Param("tableName") String tableName, @Param("ids") List<Long> ids);

    List<ChatRecordVo> chatStats(@Param("tableName") String tableName, @Param("selfId") Long selfId);

    List<ChatRecordGroup> selectWordCloudCorpus(@Param("tableName") String tableName,
                                                @Param("selfId") Long selfId,
                                                @Param("userIds") List<Long> userIds,
                                                @Param("startTime") String startTime,
                                                @Param("excludeContent") List<String> excludeList);

    List<GroupChatUserResp> selectUserInGroup(@Param("tableName") String tableName);

    List<ChatRecordPrivate> selectUserInPrivate(@Param("tableName") String tableName,
                                                @Param("keyword") String keyword,
                                                @Param("limit") Integer limit);

}
