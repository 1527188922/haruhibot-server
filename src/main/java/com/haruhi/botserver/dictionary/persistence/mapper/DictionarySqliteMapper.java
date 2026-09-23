package com.haruhi.botserver.dictionary.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.haruhi.botserver.dictionary.persistence.entity.DictionarySqlite;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DictionarySqliteMapper extends BaseMapper<DictionarySqlite> {
}
