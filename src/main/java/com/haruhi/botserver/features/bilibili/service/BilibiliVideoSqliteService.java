package com.haruhi.botserver.features.bilibili.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.haruhi.botserver.features.bilibili.client.model.bilibili.VideoDetail;
import com.haruhi.botserver.features.bilibili.model.BilibiliVideoAuthorResp;
import com.haruhi.botserver.features.bilibili.model.BilibiliVideoTagResp;
import com.haruhi.botserver.features.bilibili.persistence.entity.BilibiliVideoSqlite;

import java.util.List;

/**
 * b站视频入库
 * <p>
 * 只负责持久化，查询/下载等编排逻辑见 {@link BilibiliVideoService}
 */
public interface BilibiliVideoSqliteService extends IService<BilibiliVideoSqlite> {

    /**
     * 按 bv号+cid 查询视频
     */
    BilibiliVideoSqlite getByBvidAndCid(String bvid, Long cid);

    /**
     * 保存 getVideoDetail 的响应：bvid+cid 已存在则更新，不存在则插入。
     * <p>
     * 只更新详情接口返回的字段，不会覆盖 play_url_raw（详情接口没有该字段）。
     * @param detail 详情接口的 data
     * @param raw    详情接口的原始响应json
     * @return 入库后的记录，detail 中没有视频信息时返回 null
     */
    BilibiliVideoSqlite saveOrUpdateByVideoDetail(VideoDetail detail, String raw);

    /**
     * 保存 getPlayUrlInfo 的响应原始json，只有调用过该接口才会写入
     * @return 是否更新到了记录（记录不存在时返回false）
     */
    boolean updatePlayUrlRaw(String bvid, Long cid, String raw);

    /**
     * 所有视频作者（附带视频数），按视频数倒序，用于webui左侧分组列表
     */
    List<BilibiliVideoAuthorResp> listAuthors();

    /**
     * 所有标签（附带视频数），按视频数倒序，用于webui左侧分组列表
     */
    List<BilibiliVideoTagResp> listTags();
}
