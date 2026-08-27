package com.haruhi.botServer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.haruhi.botServer.entity.JmChapterImageSqlite;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface JmChapterImageSqliteMapper extends BaseMapper<JmChapterImageSqlite> {

    List<JmChapterImageSqlite> selectChapterList(@Param("albumId") Long albumId);

    List<JmChapterImageSqlite> selectImages(@Param("albumId") Long albumId, @Param("chapterId") Long chapterId);
}
