package com.haruhi.botserver.features.ai.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.haruhi.botserver.features.ai.persistence.entity.AiUsageEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface AiUsageMapper extends BaseMapper<AiUsageEntity> {

    /**
     * 按场景聚合；需要 join t_ai_session。
     */
    @Select("""
            SELECT s.scene AS scene,
                   COUNT(*) AS requests,
                   COALESCE(SUM(u.total_tokens), 0) AS tokens,
                   COALESCE(SUM(CASE WHEN u.success = 0 THEN 1 ELSE 0 END), 0) AS failures
            FROM t_ai_usage u
            JOIN t_ai_session s ON s.id = u.session_id
            WHERE u.created_at >= #{start}
            GROUP BY s.scene
            """)
    List<Map<String, Object>> aggregateByScene(@Param("start") String start);

    /**
     * 按群聚合，仅群聊会话。
     */
    @Select("""
            SELECT s.target_id AS target_id,
                   COUNT(*) AS requests,
                   COALESCE(SUM(u.total_tokens), 0) AS tokens,
                   COALESCE(SUM(CASE WHEN u.success = 0 THEN 1 ELSE 0 END), 0) AS failures
            FROM t_ai_usage u
            JOIN t_ai_session s ON s.id = u.session_id
            WHERE u.created_at >= #{start} AND s.scene = 'group'
            GROUP BY s.target_id
            ORDER BY tokens DESC
            LIMIT 10
            """)
    List<Map<String, Object>> aggregateByGroup(@Param("start") String start);

    /**
     * 按用户聚合，仅私聊会话。
     */
    @Select("""
            SELECT s.target_id AS target_id,
                   COUNT(*) AS requests,
                   COALESCE(SUM(u.total_tokens), 0) AS tokens,
                   COALESCE(SUM(CASE WHEN u.success = 0 THEN 1 ELSE 0 END), 0) AS failures
            FROM t_ai_usage u
            JOIN t_ai_session s ON s.id = u.session_id
            WHERE u.created_at >= #{start} AND s.scene = 'private'
            GROUP BY s.target_id
            ORDER BY tokens DESC
            LIMIT 10
            """)
    List<Map<String, Object>> aggregateByPrivate(@Param("start") String start);
}

