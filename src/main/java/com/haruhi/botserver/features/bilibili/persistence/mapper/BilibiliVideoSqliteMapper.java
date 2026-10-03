package com.haruhi.botserver.features.bilibili.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.haruhi.botserver.features.bilibili.persistence.entity.BilibiliVideoSqlite;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BilibiliVideoSqliteMapper extends BaseMapper<BilibiliVideoSqlite> {
}
