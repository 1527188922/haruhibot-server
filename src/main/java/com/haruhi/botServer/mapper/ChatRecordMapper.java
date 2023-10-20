package com.haruhi.botServer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.haruhi.botServer.entity.ChatRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ChatRecordMapper extends BaseMapper<ChatRecord> {

    /**
     * 分组统计群用户聊天记录
     * @param groupId
     * @return List<{ groupId,userId,total }>
     */
    List<ChatRecord> groupRecordCounting(@Param("groupId") Long groupId, @Param("selfId") Long selfId);
    
}
