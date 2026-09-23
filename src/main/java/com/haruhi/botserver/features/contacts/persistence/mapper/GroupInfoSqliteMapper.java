package com.haruhi.botserver.features.contacts.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.haruhi.botserver.features.contacts.persistence.entity.GroupInfoSqlite;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface GroupInfoSqliteMapper extends BaseMapper<GroupInfoSqlite> {
}
