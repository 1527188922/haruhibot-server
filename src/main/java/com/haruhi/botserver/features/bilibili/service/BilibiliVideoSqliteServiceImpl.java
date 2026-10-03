package com.haruhi.botserver.features.bilibili.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.haruhi.botserver.features.bilibili.client.model.bilibili.VideoDetail;
import com.haruhi.botserver.features.bilibili.model.BilibiliVideoAuthorResp;
import com.haruhi.botserver.features.bilibili.model.BilibiliVideoTagResp;
import com.haruhi.botserver.features.bilibili.persistence.entity.BilibiliVideoSqlite;
import com.haruhi.botserver.features.bilibili.persistence.mapper.BilibiliVideoSqliteMapper;
import com.haruhi.botserver.shared.util.DateTimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
public class BilibiliVideoSqliteServiceImpl extends ServiceImpl<BilibiliVideoSqliteMapper, BilibiliVideoSqlite>
        implements BilibiliVideoSqliteService {

    @Override
    public BilibiliVideoSqlite getByBvidAndCid(String bvid, Long cid) {
        if (StringUtils.isBlank(bvid) || Objects.isNull(cid)) {
            return null;
        }
        return this.getOne(new LambdaQueryWrapper<BilibiliVideoSqlite>()
                .eq(BilibiliVideoSqlite::getBvid, bvid)
                .eq(BilibiliVideoSqlite::getCid, cid), false);
    }

    @Override
    public BilibiliVideoSqlite saveOrUpdateByVideoDetail(VideoDetail detail, String raw) {
        if (Objects.isNull(detail) || Objects.isNull(detail.getView())) {
            return null;
        }
        VideoDetail.View view = detail.getView();
        Long cid = detail.getCidFirst() == null ? view.getCid() : detail.getCidFirst();
        if (StringUtils.isBlank(view.getBvid()) || Objects.isNull(cid)) {
            log.error("b站视频详情缺少bvid或cid，无法入库 bvid:{} cid:{}", view.getBvid(), cid);
            return null;
        }

        BilibiliVideoSqlite entity = getByBvidAndCid(view.getBvid(), cid);
        boolean insert = Objects.isNull(entity);
        if (insert) {
            entity = new BilibiliVideoSqlite();
            entity.setBvid(view.getBvid());
            entity.setCid(cid);
            entity.setCreateTime(now());
        }
        entity.setAvid(view.getAid());
        entity.setTitle(view.getTitle());
        entity.setTag(joinTags(detail.getTags()));
        entity.setDesc(view.getDesc());
        entity.setPic(view.getPic());
        entity.setDuration(view.getDuration());
        VideoDetail.View.Owner owner = view.getOwner();
        if (Objects.nonNull(owner)) {
            entity.setOwnerMid(owner.getMid());
            entity.setOwnerName(owner.getName());
            entity.setOwnerFace(owner.getFace());
        }
        entity.setPubdate(view.getPubdate());
        entity.setCtime(view.getCtime());
        entity.setVideoDetailRaw(raw);
        entity.setUpdateTime(now());

        try {
            if (insert) {
                this.save(entity);
            } else {
                // play_url_raw 不在更新字段里，详情接口的响应不会覆盖它
                this.updateById(entity);
            }
            return entity;
        } catch (DataIntegrityViolationException e) {
            // 并发下同一个视频可能同时插入了两次，改成就地更新
            BilibiliVideoSqlite exist = getByBvidAndCid(view.getBvid(), cid);
            if (Objects.isNull(exist)) {
                log.error("b站视频入库失败 bvid:{} cid:{}", view.getBvid(), cid, e);
                return null;
            }
            entity.setId(exist.getId());
            entity.setCreateTime(exist.getCreateTime());
            this.updateById(entity);
            return entity;
        }
    }

    @Override
    public boolean updatePlayUrlRaw(String bvid, Long cid, String raw) {
        if (StringUtils.isBlank(bvid) || Objects.isNull(cid) || Objects.isNull(raw)) {
            return false;
        }
        return this.update(new LambdaUpdateWrapper<BilibiliVideoSqlite>()
                .eq(BilibiliVideoSqlite::getBvid, bvid)
                .eq(BilibiliVideoSqlite::getCid, cid)
                .set(BilibiliVideoSqlite::getPlayUrlRaw, raw)
                .set(BilibiliVideoSqlite::getUpdateTime, now()));
    }

    @Override
    public List<BilibiliVideoAuthorResp> listAuthors() {
        return baseMapper.listAuthors();
    }

    @Override
    public List<BilibiliVideoTagResp> listTags() {
        List<String> rawTags = baseMapper.listAllTags();
        if (CollectionUtils.isEmpty(rawTags)) {
            return Collections.emptyList();
        }
        // 标签是逗号分隔保存的，这里拆开统计每个标签的视频数
        Map<String, Long> counter = new HashMap<>();
        for (String rawTag : rawTags) {
            if (StringUtils.isBlank(rawTag)) {
                continue;
            }
            for (String tag : rawTag.split("[,，]")) {
                String trimmed = tag.trim();
                if (StringUtils.isNotBlank(trimmed)) {
                    counter.merge(trimmed, 1L, Long::sum);
                }
            }
        }
        return counter.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .map(entry -> {
                    BilibiliVideoTagResp resp = new BilibiliVideoTagResp();
                    resp.setTag(entry.getKey());
                    resp.setVideoCount(entry.getValue());
                    return resp;
                })
                .toList();
    }

    /**
     * 标签名拼成逗号分隔的字符串
     */
    private String joinTags(List<VideoDetail.Tag> tags) {
        if (CollectionUtils.isEmpty(tags)) {
            return null;
        }
        return tags.stream()
                .filter(Objects::nonNull)
                .map(VideoDetail.Tag::getTagName)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.joining(","));
    }

    private String now() {
        return DateTimeUtil.dateTimeFormat(new Date(), DateTimeUtil.PatternEnum.yyyyMMddHHmmss);
    }
}
