package com.haruhi.botserver.infrastructure.kvstore.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.haruhi.botserver.infrastructure.kvstore.persistence.entity.KvEntry;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface KvEntryMapper extends BaseMapper<KvEntry> {
}
