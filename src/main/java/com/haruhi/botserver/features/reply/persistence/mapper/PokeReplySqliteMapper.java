package com.haruhi.botserver.features.reply.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.haruhi.botserver.features.reply.persistence.entity.PokeReplySqlite;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PokeReplySqliteMapper extends BaseMapper<PokeReplySqlite> {
}
