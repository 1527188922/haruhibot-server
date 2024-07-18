package com.haruhi.botServer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.haruhi.botServer.entity.CustomReply;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CustomReplyMapper extends BaseMapper<CustomReply> {
}
