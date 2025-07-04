package com.haruhi.botServer.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.haruhi.botServer.config.DataBaseConfig;
import com.haruhi.botServer.entity.CustomReply;
import org.apache.ibatis.annotations.Mapper;

@Mapper
@DS(DataBaseConfig.DATA_SOURCE_MYSQL)
public interface CustomReplyMapper extends BaseMapper<CustomReply> {
}
