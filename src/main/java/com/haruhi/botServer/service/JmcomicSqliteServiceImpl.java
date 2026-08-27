package com.haruhi.botServer.service;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.haruhi.botServer.dto.jmcomic.Album;
import com.haruhi.botServer.dto.jmcomic.Chapter;
import com.haruhi.botServer.dto.jmcomic.Series;
import com.haruhi.botServer.entity.JmAlbumSqlite;
import com.haruhi.botServer.entity.JmChapterImageSqlite;
import com.haruhi.botServer.mapper.JmAlbumSqliteMapper;
import com.haruhi.botServer.mapper.JmChapterImageSqliteMapper;
import com.haruhi.botServer.utils.DateTimeUtil;
import com.haruhi.botServer.vo.JmChapterImageResp;
import com.haruhi.botServer.vo.JmChapterInfoResp;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class JmcomicSqliteServiceImpl implements JmcomicSqliteService {

    @Autowired
    private JmAlbumSqliteMapper jmAlbumSqliteMapper;

    @Autowired
    private JmChapterImageSqliteMapper jmChapterImageSqliteMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveOrUpdateAlbum(Album album, String raw) {
        if (album == null || album.getId() == null) {
            return;
        }
        JmAlbumSqlite entity = toAlbumEntity(album, raw);
        String now = DateTimeUtil.dateTimeFormat(new Date(), DateTimeUtil.PatternEnum.yyyyMMddHHmmss);
        entity.setModifyTime(now);
        JmAlbumSqlite exist = jmAlbumSqliteMapper.selectById(entity.getId());
        if (exist == null) {
            entity.setCreateTime(now);
            jmAlbumSqliteMapper.insert(entity);
            return;
        }
        entity.setCreateTime(StringUtils.isNotBlank(exist.getCreateTime()) ? exist.getCreateTime() : now);
        jmAlbumSqliteMapper.deleteById(entity.getId());
        jmAlbumSqliteMapper.insert(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveOrUpdateChapterImages(Long albumId, Chapter chapter) {
        saveOrUpdateChapterImages(albumId, chapter, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveOrUpdateChapterImages(Long albumId, Chapter chapter, Series series) {
        if (albumId == null || chapter == null || chapter.getId() == null) {
            return;
        }
        jmChapterImageSqliteMapper.delete(new LambdaUpdateWrapper<JmChapterImageSqlite>()
                .eq(JmChapterImageSqlite::getAlbumId, albumId)
                .eq(JmChapterImageSqlite::getChapterId, chapter.getId()));
        List<JmChapterImageSqlite> images = toChapterImageEntities(albumId, chapter, series);
        if (CollectionUtils.isEmpty(images)) {
            return;
        }
        images.forEach(jmChapterImageSqliteMapper::insert);
    }

    @Override
    public List<JmChapterInfoResp> listChapters(Long albumId) {
        if (albumId == null) {
            return Collections.emptyList();
        }
        JmAlbumSqlite album = jmAlbumSqliteMapper.selectById(albumId);
        if (album != null && StringUtils.isNotBlank(album.getSeries())) {
            List<Series> series = JSONObject.parseObject(album.getSeries(), new TypeReference<List<Series>>() {});
            if (CollectionUtils.isNotEmpty(series)) {
                return series.stream()
                        .filter(e -> StringUtils.isNotBlank(e.getId()))
                        .map(e -> toChapterInfoResp(albumId, e))
                        .collect(Collectors.toList());
            }
        }
        return jmChapterImageSqliteMapper.selectChapterList(albumId).stream()
                .map(this::toChapterInfoResp)
                .collect(Collectors.toList());
    }

    @Override
    public List<JmChapterImageResp> listChapterImages(Long albumId, Long chapterId) {
        if (albumId == null || chapterId == null) {
            return Collections.emptyList();
        }
        return jmChapterImageSqliteMapper.selectImages(albumId, chapterId).stream()
                .map(this::toChapterImageResp)
                .collect(Collectors.toList());
    }

    JmAlbumSqlite toAlbumEntity(Album album, String raw) {
        JmAlbumSqlite entity = new JmAlbumSqlite();
        entity.setId(album.getId());
        entity.setName(album.getName());
        entity.setAlbumFolderName(album.getAlbumFolderName());
        entity.setImages(toJson(album.getImages()));
        entity.setAddTime(album.getAddTime());
        entity.setDescription(album.getDescription());
        entity.setTotalViews(album.getTotalViews());
        entity.setLikes(album.getLikes());
        entity.setSeries(toJson(album.getSeries()));
        entity.setSeriesId(album.getSeriesId());
        entity.setCommentTotal(album.getCommentTotal());
        entity.setAuthor(toJson(album.getAuthor()));
        entity.setTags(toJson(album.getTags()));
        entity.setWorks(toJson(album.getWorks()));
        entity.setActors(toJson(album.getActors()));
        entity.setRelatedList(toJson(album.getRelatedList()));
        entity.setLiked(album.getLiked());
        entity.setIsFavorite(album.getIsFavorite());
        entity.setIsAids(album.getIsAids());
        entity.setPrice(album.getPrice());
        entity.setPurchased(album.getPurchased());
        entity.setRaw(raw);
        return entity;
    }

    List<JmChapterImageSqlite> toChapterImageEntities(Long albumId, Chapter chapter, Series series) {
        if (CollectionUtils.isEmpty(chapter.getImages())) {
            return Collections.emptyList();
        }
        List<String> sortedImages = JmcomicService.sortImageFiles(chapter.getImages());
        return IntStream.range(0, sortedImages.size()).mapToObj(i -> {
            String image = sortedImages.get(i);
            JmChapterImageSqlite entity = new JmChapterImageSqlite();
            entity.setAlbumId(albumId);
            entity.setChapterId(chapter.getId());
            entity.setChapterSort(series == null ? null : series.getSort());
            entity.setChapterTitle(series == null ? null : series.getTitle());
            entity.setChapterName(chapter.getName());
            entity.setChapterAddTime(chapter.getAddTime());
            entity.setSeriesId(chapter.getSeriesId());
            entity.setLiked(chapter.getLiked());
            entity.setIsFavorite(chapter.getIsFavorite());
            entity.setImageFile(image);
            entity.setImageSort(i + 1);
            return entity;
        }).collect(Collectors.toList());
    }

    JmChapterImageResp toChapterImageResp(JmChapterImageSqlite image) {
        JmChapterImageResp resp = new JmChapterImageResp();
        resp.setAlbumId(image.getAlbumId());
        resp.setChapterId(image.getChapterId());
        resp.setChapterSort(image.getChapterSort());
        resp.setChapterTitle(image.getChapterTitle());
        resp.setChapterName(image.getChapterName());
        resp.setChapterAddTime(image.getChapterAddTime());
        resp.setSeriesId(image.getSeriesId());
        resp.setImageFile(image.getImageFile());
        resp.setImageSort(image.getImageSort());
        resp.setImgUrl(JmcomicService.buildImgUrl(image.getChapterId(), image.getImageFile()));
        return resp;
    }

    private JmChapterInfoResp toChapterInfoResp(Long albumId, Series series) {
        JmChapterInfoResp resp = new JmChapterInfoResp();
        resp.setAlbumId(albumId);
        resp.setChapterId(Long.valueOf(series.getId()));
        resp.setName(series.getName());
        resp.setSort(series.getSort());
        resp.setTitle(StringUtils.isNotBlank(series.getTitle()) ? series.getTitle() : "第" + series.getSort() + "话");
        return resp;
    }

    private JmChapterInfoResp toChapterInfoResp(JmChapterImageSqlite image) {
        JmChapterInfoResp resp = new JmChapterInfoResp();
        resp.setAlbumId(image.getAlbumId());
        resp.setChapterId(image.getChapterId());
        resp.setSort(image.getChapterSort());
        resp.setName(image.getChapterName());
        resp.setTitle(StringUtils.isNotBlank(image.getChapterTitle()) ? image.getChapterTitle() : image.getChapterName());
        return resp;
    }

    private String toJson(Object value) {
        if (Objects.isNull(value)) {
            return null;
        }
        return JSONObject.toJSONString(value);
    }
}
