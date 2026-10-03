package com.haruhi.botserver.features.bilibili.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.haruhi.botserver.features.bilibili.model.BilibiliVideoAuthorResp;
import com.haruhi.botserver.features.bilibili.persistence.entity.BilibiliVideoSqlite;
import com.haruhi.botserver.infrastructure.persistence.DataBaseConst;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface BilibiliVideoSqliteMapper extends BaseMapper<BilibiliVideoSqlite> {

    /**
     * 视频作者分组列表，按视频数倒序。
     * <p>
     * 同一个mid的昵称/头像每次入库都会刷新成最新值，这里用max取组内任意一个即可
     */
    @Select("SELECT owner_mid AS owner_mid, MAX(owner_name) AS owner_name, MAX(owner_face) AS owner_face,"
            + " COUNT(*) AS video_count FROM `" + DataBaseConst.T_BILIBILI_VIDEO + "`"
            + " WHERE owner_mid IS NOT NULL GROUP BY owner_mid ORDER BY video_count DESC")
    List<BilibiliVideoAuthorResp> listAuthors();

    /**
     * 所有非空的标签串（逗号分隔），在java侧拆分统计出现次数
     */
    @Select("SELECT tag FROM `" + DataBaseConst.T_BILIBILI_VIDEO + "` WHERE tag IS NOT NULL AND tag != ''")
    List<String> listAllTags();
}
