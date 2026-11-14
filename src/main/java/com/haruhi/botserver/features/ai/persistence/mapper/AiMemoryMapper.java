package com.haruhi.botserver.features.ai.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.haruhi.botserver.features.ai.persistence.entity.AiMemoryEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AiMemoryMapper extends BaseMapper<AiMemoryEntity> {
}
