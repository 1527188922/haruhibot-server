package com.haruhi.botserver.infrastructure.persistence.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.haruhi.botserver.infrastructure.persistence.persistence.entity.DictionarySqlite;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DictionarySqliteMapper extends BaseMapper<DictionarySqlite> {
}
