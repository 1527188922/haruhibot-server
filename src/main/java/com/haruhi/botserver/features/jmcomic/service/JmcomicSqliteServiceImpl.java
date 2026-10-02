package com.haruhi.botserver.features.jmcomic.service;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.haruhi.botserver.bootstrap.WebResourceConfig;
import com.haruhi.botserver.infrastructure.persistence.DataBaseConst;
import com.haruhi.botserver.features.jmcomic.client.model.Album;
import com.haruhi.botserver.features.jmcomic.client.model.Chapter;
import com.haruhi.botserver.features.jmcomic.client.model.Series;
import com.haruhi.botserver.features.jmcomic.persistence.entity.JmAlbumSqlite;
import com.haruhi.botserver.features.jmcomic.persistence.entity.JmChapterImageSqlite;
import com.haruhi.botserver.features.jmcomic.persistence.entity.JmFavoriteAlbumSqlite;
import com.haruhi.botserver.features.jmcomic.persistence.entity.JmFavoriteSqlite;
import com.haruhi.botserver.shared.error.BusinessException;
import com.haruhi.botserver.features.jmcomic.persistence.mapper.JmAlbumSqliteMapper;
import com.haruhi.botserver.features.jmcomic.persistence.mapper.JmChapterImageSqliteMapper;
import com.haruhi.botserver.features.jmcomic.persistence.mapper.JmFavoriteAlbumSqliteMapper;
import com.haruhi.botserver.features.jmcomic.persistence.mapper.JmFavoriteSqliteMapper;
import com.haruhi.botserver.shared.util.DateTimeUtil;
import com.haruhi.botserver.shared.util.FileUtil;
import com.haruhi.botserver.features.jmcomic.model.JmAlbumDeleteReq;
import com.haruhi.botserver.features.jmcomic.model.JmAlbumCollectReq;
import com.haruhi.botserver.features.jmcomic.model.JmAlbumManageResp;
import com.haruhi.botserver.features.jmcomic.model.JmAlbumQueryReq;
import com.haruhi.botserver.features.jmcomic.model.JmChapterImageDeleteReq;
import com.haruhi.botserver.features.jmcomic.model.JmChapterImageManageResp;
import com.haruhi.botserver.features.jmcomic.model.JmChapterImageQueryReq;
import com.haruhi.botserver.features.jmcomic.model.JmChapterImageResp;
import com.haruhi.botserver.features.jmcomic.model.JmChapterInfoResp;
import com.haruhi.botserver.features.jmcomic.model.JmFavoriteAlbumRemoveReq;
import com.haruhi.botserver.features.jmcomic.model.JmFavoriteAlbumReq;
import com.haruhi.botserver.features.jmcomic.model.JmFavoriteResp;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Service
@Slf4j
public class JmcomicSqliteServiceImpl implements JmcomicSqliteService {

    private static final Set<String> IMAGE_EXTENSIONS = new HashSet<>(Arrays.asList("jpg", "jpeg", "png", "gif", "webp", "bmp"));

    /**
     * 默认收藏夹名称。初始化时由 SqliteDatabaseService 写入，不允许删除/重命名。
     * 收藏时不指定收藏夹、或漫画被移出所有收藏夹时，都会回落到这里。
     */
    public static final String DEFAULT_FAVORITE_NAME = "默认收藏夹";

    @Autowired
    private JmAlbumSqliteMapper jmAlbumSqliteMapper;

    @Autowired
    private JmChapterImageSqliteMapper jmChapterImageSqliteMapper;

    @Autowired
    private JmFavoriteSqliteMapper jmFavoriteSqliteMapper;

    @Autowired
    private JmFavoriteAlbumSqliteMapper jmFavoriteAlbumSqliteMapper;

    @Autowired
    private WebResourceConfig webResourceConfig;

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
        // collected是本地收藏标记，JM响应里没有这个字段，这里是从旧记录带过来的，否则delete+insert会丢掉收藏状态
        entity.setCollected(exist.getCollected());
        jmAlbumSqliteMapper.deleteById(entity.getId());
        jmAlbumSqliteMapper.insert(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveOrUpdateChapterImages(Long albumId, Chapter chapter) {
        saveOrUpdateChapterImages(albumId, chapter, chapter == null ? null : findSeries(albumId, chapter.getId()));
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
        return this.listChapters(album);
    }

    public List<JmChapterInfoResp> listChapters(JmAlbumSqlite album) {
        if (album == null || album.getId() == null) {
            return Collections.emptyList();
        }
        if ("[]".equals(album.getSeries()) || StringUtils.isBlank(album.getSeries())) {
            return jmChapterImageSqliteMapper.selectChapterList(album.getId()).stream()
                    .map(this::toChapterInfoResp)
                    .toList();
        }
        List<Series> series = JSONObject.parseObject(album.getSeries(), new TypeReference<>() {});
        if (CollectionUtils.isNotEmpty(series)) {
            return series.stream()
                    .filter(e -> StringUtils.isNotBlank(e.getId()))
                    .map(e -> toChapterInfoResp(album.getId(), e))
                    .toList();
        }
        return Collections.emptyList();
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

    @Override
    public IPage<JmAlbumManageResp> searchAlbums(JmAlbumQueryReq request) {
        if (request == null) {
            request = new JmAlbumQueryReq();
        }
        LambdaQueryWrapper<JmAlbumSqlite> queryWrapper = new LambdaQueryWrapper<JmAlbumSqlite>()
                .eq(Objects.nonNull(request.getId()), JmAlbumSqlite::getId, request.getId())
                .like(StringUtils.isNotBlank(request.getName()), JmAlbumSqlite::getName, request.getName())
                .like(StringUtils.isNotBlank(request.getAuthor()), JmAlbumSqlite::getAuthor, request.getAuthor())
                // 指定收藏夹时以收藏夹为准：收藏夹本身已隐含"已收藏"，不再叠加 collected 条件
                .eq(request.getFavoriteId() == null && Objects.nonNull(request.getCollected()),
                        JmAlbumSqlite::getCollected, request.getCollected())
                .orderByDesc(JmAlbumSqlite::getCreateTime);
        applyTagFilter(queryWrapper, request);
        applyFavoriteFilter(queryWrapper, request.getFavoriteId());
        IPage<JmAlbumSqlite> sourcePage = jmAlbumSqliteMapper.selectPage(new Page<>(request.getCurrentPage(), request.getPageSize()), queryWrapper);
        Page<JmAlbumManageResp> targetPage = new Page<>(sourcePage.getCurrent(), sourcePage.getSize(), sourcePage.getTotal());
        List<JmAlbumManageResp> records = sourcePage.getRecords().stream()
                .map(this::toAlbumManageResp)
                .collect(Collectors.toList());
        fillFavoriteIds(records, request.getFavoriteId());
        targetPage.setRecords(records);
        return targetPage;
    }

    /**
     * 只看指定收藏夹下的漫画。
     * 用 EXISTS 子查询而不是 JOIN，避免与分页 count 语句产生重复行。
     */
    private void applyFavoriteFilter(LambdaQueryWrapper<JmAlbumSqlite> query, Long favoriteId) {
        if (favoriteId == null) {
            return;
        }
        query.apply("EXISTS (SELECT 1 FROM `" + DataBaseConst.T_JM_FAVORITE_ALBUM
                + "` fa WHERE fa.album_id = `" + DataBaseConst.T_JM_ALBUM
                + "`.id AND fa.favorite_id = {0})", favoriteId);
    }

    /**
     * 批量回填每条记录所属的收藏夹id（避免逐条查询）。
     * 指定了收藏夹查询时，顺带回填"加入该收藏夹的时间"，供收藏夹 tab 列表展示。
     */
    private void fillFavoriteIds(List<JmAlbumManageResp> records, Long filterFavoriteId) {
        if (CollectionUtils.isEmpty(records)) {
            return;
        }
        List<Long> albumIds = records.stream()
                .map(JmAlbumManageResp::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (albumIds.isEmpty()) {
            return;
        }
        List<JmFavoriteAlbumSqlite> relations = jmFavoriteAlbumSqliteMapper.selectList(
                new LambdaQueryWrapper<JmFavoriteAlbumSqlite>()
                        .in(JmFavoriteAlbumSqlite::getAlbumId, albumIds));
        Map<Long, List<Long>> favoriteIdMap = relations.stream()
                .collect(Collectors.groupingBy(JmFavoriteAlbumSqlite::getAlbumId,
                        Collectors.mapping(JmFavoriteAlbumSqlite::getFavoriteId, Collectors.toList())));
        Map<Long, String> addTimeMap = filterFavoriteId == null ? Collections.emptyMap() : relations.stream()
                .filter(relation -> Objects.equals(filterFavoriteId, relation.getFavoriteId()))
                .collect(Collectors.toMap(JmFavoriteAlbumSqlite::getAlbumId,
                        relation -> formatFavoriteAddTime(relation.getCreateTime()), (first, second) -> first));
        records.forEach(record -> {
            record.setFavoriteIds(favoriteIdMap.getOrDefault(record.getId(), Collections.emptyList()));
            record.setFavoriteAddTime(addTimeMap.get(record.getId()));
        });
    }

    /**
     * 关联表里存的是 yyyyMMddHHmmss，这里转成便于前端直接展示的 yyyy-MM-dd HH:mm:ss
     */
    private String formatFavoriteAddTime(String value) {
        String text = StringUtils.trimToEmpty(value);
        if (text.length() < 14) {
            return text;
        }
        return text.substring(0, 4) + "-" + text.substring(4, 6) + "-" + text.substring(6, 8)
                + " " + text.substring(8, 10) + ":" + text.substring(10, 12) + ":" + text.substring(12, 14);
    }

    private static void applyTagFilter(LambdaQueryWrapper<JmAlbumSqlite> query, JmAlbumQueryReq reqVO) {
        List<String> tags = reqVO.getTags();
        if (tags == null || tags.isEmpty()) {
            return;
        }
        List<String> validTags = tags.stream()
                .filter(StringUtils::isNotBlank)
                .map(String::trim)
                .distinct()
                .toList();
        if (validTags.isEmpty()) {
            return;
        }
        query.and(wrapper -> {
            for (int i = 0; i < validTags.size(); i++) {
                if (i > 0) {
                    wrapper.or();
                }
                wrapper.apply("INSTR(tags, {0}) > 0",
                        "\"" + validTags.get(i) + "\"");
            }
        });
    }

    @Override
    public List<String> allTag() {
        return allJsonArrayField(JmAlbumSqlite::getTags);
    }

    @Override
    public List<String> allAuthor() {
        return allJsonArrayField(JmAlbumSqlite::getAuthor);
    }

    private List<String> allJsonArrayField(SFunction<JmAlbumSqlite, String> columnGetter) {
        List<JmAlbumSqlite> jmAlbumSqlites = this.jmAlbumSqliteMapper.selectList(new LambdaQueryWrapper<JmAlbumSqlite>()
                        .select(columnGetter)
                        .isNotNull(columnGetter));
        if (jmAlbumSqlites.isEmpty()) {
            return Collections.emptyList();
        }
        return jmAlbumSqlites.stream()
                .map(columnGetter)
                .filter(StringUtils::isNotBlank)
                .flatMap(this::parseJsonArray)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .sorted()
                .toList();
    }

    private Stream<String> parseJsonArray(String json) {
        try {
            List<String> values = JSONObject.parseObject(json, new TypeReference<List<String>>() {});
            return values == null ? Stream.empty() : values.stream();
        } catch (Exception e) {
            return Stream.empty();
        }
    }

    @Override
    public Set<Long> existsAlbumIds(Collection<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return Collections.emptySet();
        }
        return jmAlbumSqliteMapper.selectList(new LambdaQueryWrapper<JmAlbumSqlite>()
                        .select(JmAlbumSqlite::getId)
                        .in(JmAlbumSqlite::getId, ids))
                .stream()
                .map(JmAlbumSqlite::getId)
                .collect(Collectors.toSet());
    }

    @Override
    public String findAlbumName(Long id) {
        if (id == null) {
            return null;
        }
        JmAlbumSqlite album = jmAlbumSqliteMapper.selectOne(new LambdaQueryWrapper<JmAlbumSqlite>()
                .select(JmAlbumSqlite::getName)
                .eq(JmAlbumSqlite::getId, id));
        return album == null ? null : album.getName();
    }

    @Override
    public IPage<JmChapterImageManageResp> searchChapterImages(JmChapterImageQueryReq request) {
        if (request == null) {
            request = new JmChapterImageQueryReq();
        }
        LambdaQueryWrapper<JmChapterImageSqlite> queryWrapper = new LambdaQueryWrapper<JmChapterImageSqlite>()
                .eq(Objects.nonNull(request.getAlbumId()), JmChapterImageSqlite::getAlbumId, request.getAlbumId())
                .eq(Objects.nonNull(request.getChapterId()), JmChapterImageSqlite::getChapterId, request.getChapterId())
                .like(StringUtils.isNotBlank(request.getChapterTitle()), JmChapterImageSqlite::getChapterTitle, request.getChapterTitle())
                .like(StringUtils.isNotBlank(request.getImageFile()), JmChapterImageSqlite::getImageFile, request.getImageFile())
                .orderByAsc(JmChapterImageSqlite::getAlbumId)
                .orderByAsc(JmChapterImageSqlite::getChapterSort)
                .orderByAsc(JmChapterImageSqlite::getImageSort);
        IPage<JmChapterImageSqlite> sourcePage = jmChapterImageSqliteMapper.selectPage(new Page<>(request.getCurrentPage(), request.getPageSize()), queryWrapper);
        Page<JmChapterImageManageResp> targetPage = new Page<>(sourcePage.getCurrent(), sourcePage.getSize(), sourcePage.getTotal());
        List<Long> albumIds = sourcePage.getRecords().stream()
                .map(JmChapterImageSqlite::getAlbumId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, JmAlbumSqlite> albumMap = CollectionUtils.isEmpty(albumIds) ? Collections.emptyMap()
                : jmAlbumSqliteMapper.selectByIds(albumIds).stream().collect(Collectors.toMap(JmAlbumSqlite::getId, e -> e));
        targetPage.setRecords(sourcePage.getRecords().stream()
                .map(e -> toChapterImageManageResp(e, albumMap.get(e.getAlbumId())))
                .collect(Collectors.toList()));
        return targetPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAlbums(JmAlbumDeleteReq request) {
        if (request == null || CollectionUtils.isEmpty(request.getIds())) {
            return;
        }
        List<JmAlbumSqlite> albums = jmAlbumSqliteMapper.selectByIds(request.getIds());
        for (JmAlbumSqlite album : albums) {
            if (Boolean.TRUE.equals(request.getDeleteZip())) {
                deleteFileUnderJmcomic(getZipFile(album));
            }
            if (Boolean.TRUE.equals(request.getDeletePdf())) {
                deleteFileUnderJmcomic(getPdfFile(album));
            }
            if (Boolean.TRUE.equals(request.getDeleteImages())) {
                deleteDirectoryQuietly(getAlbumDir(album));
            }
        }
        if (Boolean.TRUE.equals(request.getDeleteData())) {
            jmChapterImageSqliteMapper.delete(new LambdaQueryWrapper<JmChapterImageSqlite>()
                    .in(JmChapterImageSqlite::getAlbumId, request.getIds()));
            jmAlbumSqliteMapper.deleteByIds(request.getIds());
        }
    }

    /**
     * 收藏/取消收藏JM主记录。
     * 收藏：加入目标收藏夹（未指定则默认收藏夹，名称为新名称时自动新建），并置 collected=true；
     * 取消：从所有收藏夹移出，并置 collected=false。
     * 只更新 collected 列与关联表，不动 modifyTime，避免打乱列表默认的修改时间排序。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void collectAlbums(JmAlbumCollectReq request) {
        if (request == null || CollectionUtils.isEmpty(request.getIds())) {
            return;
        }
        List<Long> ids = request.getIds().stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return;
        }
        if (!Boolean.TRUE.equals(request.getCollected())) {
            // 取消收藏 = 从所有收藏夹移出
            jmFavoriteAlbumSqliteMapper.delete(new LambdaQueryWrapper<JmFavoriteAlbumSqlite>()
                    .in(JmFavoriteAlbumSqlite::getAlbumId, ids));
            setCollectedFlag(ids, false);
            return;
        }
        // 支持一次收藏到多个收藏夹（多选）
        resolveFavoriteIds(request.getFavoriteId(), request.getFavoriteName(),
                request.getFavoriteIds(), request.getFavoriteNames(), true)
                .forEach(favoriteId -> addAlbumsToFavoriteInternal(favoriteId, ids));
        setCollectedFlag(ids, true);
    }

    @Override
    public List<JmFavoriteResp> listFavorites() {
        List<JmFavoriteSqlite> favorites = jmFavoriteSqliteMapper.selectList(
                new LambdaQueryWrapper<JmFavoriteSqlite>()
                        .orderByAsc(JmFavoriteSqlite::getSortOrder)
                        .orderByAsc(JmFavoriteSqlite::getId));
        if (favorites.isEmpty()) {
            return Collections.emptyList();
        }
        // 一次查询统计所有收藏夹的数量，避免逐个 count
        Map<Long, Long> countMap = jmFavoriteAlbumSqliteMapper.selectList(
                        new LambdaQueryWrapper<JmFavoriteAlbumSqlite>()
                                .select(JmFavoriteAlbumSqlite::getFavoriteId))
                .stream()
                .collect(Collectors.groupingBy(JmFavoriteAlbumSqlite::getFavoriteId, Collectors.counting()));
        return favorites.stream().map(favorite -> {
            JmFavoriteResp resp = new JmFavoriteResp();
            BeanUtils.copyProperties(favorite, resp);
            resp.setAlbumCount(countMap.getOrDefault(favorite.getId(), 0L));
            resp.setIsDefault(isDefaultFavorite(favorite));
            return resp;
        })
                // 默认收藏夹始终排在最上方，其余按漫画数量降序（数量相同按id升序，保证顺序稳定）
                .sorted(Comparator.comparing(JmFavoriteResp::getIsDefault, Comparator.reverseOrder())
                        .thenComparing(JmFavoriteResp::getAlbumCount, Comparator.reverseOrder())
                        .thenComparing(JmFavoriteResp::getId))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JmFavoriteResp createFavorite(String name) {
        String trimmed = normalizeFavoriteName(name);
        if (findFavoriteByName(trimmed) != null) {
            throw new BusinessException("收藏夹「" + trimmed + "」已存在");
        }
        JmFavoriteSqlite entity = new JmFavoriteSqlite();
        entity.setName(trimmed);
        entity.setSortOrder(nextFavoriteSortOrder());
        String now = DateTimeUtil.dateTimeFormat(new Date(), DateTimeUtil.PatternEnum.yyyyMMddHHmmss);
        entity.setCreateTime(now);
        entity.setModifyTime(now);
        jmFavoriteSqliteMapper.insert(entity);
        return toFavoriteResp(entity, 0L);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JmFavoriteResp renameFavorite(Long id, String name) {
        JmFavoriteSqlite favorite = requireFavorite(id);
        if (isDefaultFavorite(favorite)) {
            throw new BusinessException("默认收藏夹不支持重命名");
        }
        String trimmed = normalizeFavoriteName(name);
        JmFavoriteSqlite sameName = findFavoriteByName(trimmed);
        if (sameName != null && !Objects.equals(sameName.getId(), id)) {
            throw new BusinessException("收藏夹「" + trimmed + "」已存在");
        }
        favorite.setName(trimmed);
        favorite.setModifyTime(DateTimeUtil.dateTimeFormat(new Date(), DateTimeUtil.PatternEnum.yyyyMMddHHmmss));
        jmFavoriteSqliteMapper.updateById(favorite);
        Long albumCount = jmFavoriteAlbumSqliteMapper.selectCount(new LambdaQueryWrapper<JmFavoriteAlbumSqlite>()
                .eq(JmFavoriteAlbumSqlite::getFavoriteId, id));
        return toFavoriteResp(favorite, albumCount);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFavorite(Long id) {
        JmFavoriteSqlite favorite = requireFavorite(id);
        if (isDefaultFavorite(favorite)) {
            throw new BusinessException("默认收藏夹不支持删除");
        }
        // 先取出该收藏夹下的漫画，删除后把这些漫画回落到默认收藏夹，避免它们"凭空取消收藏"
        List<Long> albumIds = jmFavoriteAlbumSqliteMapper.selectList(
                        new LambdaQueryWrapper<JmFavoriteAlbumSqlite>()
                                .select(JmFavoriteAlbumSqlite::getAlbumId)
                                .eq(JmFavoriteAlbumSqlite::getFavoriteId, id))
                .stream()
                .map(JmFavoriteAlbumSqlite::getAlbumId)
                .distinct()
                .toList();
        jmFavoriteAlbumSqliteMapper.delete(new LambdaQueryWrapper<JmFavoriteAlbumSqlite>()
                .eq(JmFavoriteAlbumSqlite::getFavoriteId, id));
        jmFavoriteSqliteMapper.deleteById(id);
        if (albumIds.isEmpty()) {
            return;
        }
        Long defaultFavoriteId = getOrCreateDefaultFavoriteId();
        addAlbumsToFavoriteInternal(defaultFavoriteId, albumIds);
        // 这些漫画仍在默认收藏夹中，collected 保持 true
        setCollectedFlag(albumIds, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addAlbumsToFavorite(JmFavoriteAlbumReq request) {
        if (request == null || CollectionUtils.isEmpty(request.getAlbumIds())) {
            return;
        }
        List<Long> ids = request.getAlbumIds().stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return;
        }
        // 支持一次加入多个收藏夹（多选）
        resolveFavoriteIds(request.getFavoriteId(), request.getFavoriteName(),
                request.getFavoriteIds(), request.getFavoriteNames(), true)
                .forEach(favoriteId -> addAlbumsToFavoriteInternal(favoriteId, ids));
        setCollectedFlag(ids, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveAlbumFavorites(JmFavoriteAlbumReq request) {
        if (request == null || CollectionUtils.isEmpty(request.getAlbumIds())) {
            return;
        }
        List<Long> ids = request.getAlbumIds().stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return;
        }
        // 多选在前端是"勾选即属于"，所以这里不使用默认收藏夹兜底：
        // 不传任何一个收藏夹表示"从所有收藏夹移出"，与取消收藏等价
        LinkedHashSet<Long> targetFavoriteIds = resolveFavoriteIds(request.getFavoriteId(), request.getFavoriteName(),
                request.getFavoriteIds(), request.getFavoriteNames(), false);
        jmFavoriteAlbumSqliteMapper.delete(new LambdaQueryWrapper<JmFavoriteAlbumSqlite>()
                .in(JmFavoriteAlbumSqlite::getAlbumId, ids)
                .notIn(!targetFavoriteIds.isEmpty(), JmFavoriteAlbumSqlite::getFavoriteId, targetFavoriteIds));
        targetFavoriteIds.forEach(favoriteId -> addAlbumsToFavoriteInternal(favoriteId, ids));
        setCollectedFlag(ids, !targetFavoriteIds.isEmpty());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeAlbumsFromFavorite(JmFavoriteAlbumRemoveReq request) {
        if (request == null || CollectionUtils.isEmpty(request.getAlbumIds())) {
            return;
        }
        List<Long> ids = request.getAlbumIds().stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return;
        }
        jmFavoriteAlbumSqliteMapper.delete(new LambdaQueryWrapper<JmFavoriteAlbumSqlite>()
                .in(JmFavoriteAlbumSqlite::getAlbumId, ids)
                .eq(request.getFavoriteId() != null, JmFavoriteAlbumSqlite::getFavoriteId, request.getFavoriteId()));
        // 移出后仍属于其他收藏夹的，collected 保持 true
        Set<Long> stillCollected = jmFavoriteAlbumSqliteMapper.selectList(
                        new LambdaQueryWrapper<JmFavoriteAlbumSqlite>()
                                .select(JmFavoriteAlbumSqlite::getAlbumId)
                                .in(JmFavoriteAlbumSqlite::getAlbumId, ids))
                .stream()
                .map(JmFavoriteAlbumSqlite::getAlbumId)
                .collect(Collectors.toSet());
        List<Long> lostAll = ids.stream().filter(id -> !stillCollected.contains(id)).toList();
        if (!lostAll.isEmpty()) {
            setCollectedFlag(lostAll, false);
        }
    }

    /* ==================== 收藏夹内部辅助方法 ==================== */

    private void setCollectedFlag(List<Long> albumIds, boolean collected) {
        jmAlbumSqliteMapper.update(null, new LambdaUpdateWrapper<JmAlbumSqlite>()
                .in(JmAlbumSqlite::getId, albumIds)
                .set(JmAlbumSqlite::getCollected, collected));
    }

    /**
     * 把漫画加入收藏夹，已存在的关联跳过（避免触发唯一索引冲突）
     */
    private void addAlbumsToFavoriteInternal(Long favoriteId, List<Long> albumIds) {
        if (favoriteId == null || CollectionUtils.isEmpty(albumIds)) {
            return;
        }
        Set<Long> exists = jmFavoriteAlbumSqliteMapper.selectList(
                        new LambdaQueryWrapper<JmFavoriteAlbumSqlite>()
                                .select(JmFavoriteAlbumSqlite::getAlbumId)
                                .eq(JmFavoriteAlbumSqlite::getFavoriteId, favoriteId)
                                .in(JmFavoriteAlbumSqlite::getAlbumId, albumIds))
                .stream()
                .map(JmFavoriteAlbumSqlite::getAlbumId)
                .collect(Collectors.toSet());
        String now = DateTimeUtil.dateTimeFormat(new Date(), DateTimeUtil.PatternEnum.yyyyMMddHHmmss);
        albumIds.stream()
                .filter(albumId -> !exists.contains(albumId))
                .forEach(albumId -> {
                    JmFavoriteAlbumSqlite entity = new JmFavoriteAlbumSqlite();
                    entity.setFavoriteId(favoriteId);
                    entity.setAlbumId(albumId);
                    entity.setCreateTime(now);
                    jmFavoriteAlbumSqliteMapper.insert(entity);
                });
    }

    /**
     * 解析本次操作涉及的收藏夹id集合，支持多选：
     * <ul>
     *     <li>单选：favoriteId 优先于 favoriteName，名称不存在时新建</li>
     *     <li>多选：favoriteIds / favoriteNames 逐个解析，名称不存在时新建</li>
     *     <li>单选与多选同时传时取并集</li>
     *     <li>都没传：useDefaultWhenEmpty=true 时回落到默认收藏夹，否则返回空集合</li>
     * </ul>
     * 用 LinkedHashSet 保证顺序（先多选后单选）且天然去重。
     */
    private LinkedHashSet<Long> resolveFavoriteIds(Long favoriteId, String favoriteName,
                                                   List<Long> favoriteIds, List<String> favoriteNames,
                                                   boolean useDefaultWhenEmpty) {
        LinkedHashSet<Long> target = new LinkedHashSet<>();
        if (favoriteIds != null) {
            favoriteIds.stream().filter(Objects::nonNull).forEach(target::add);
        }
        if (favoriteNames != null) {
            favoriteNames.stream()
                    .filter(StringUtils::isNotBlank)
                    .map(this::getOrCreateFavoriteIdByName)
                    .forEach(target::add);
        }
        if (favoriteId != null) {
            target.add(requireFavorite(favoriteId).getId());
        } else if (StringUtils.isNotBlank(favoriteName)) {
            target.add(getOrCreateFavoriteIdByName(favoriteName));
        }
        if (target.isEmpty() && useDefaultWhenEmpty) {
            target.add(getOrCreateDefaultFavoriteId());
        }
        return target;
    }

    private Long getOrCreateFavoriteIdByName(String name) {
        String trimmed = normalizeFavoriteName(name);
        JmFavoriteSqlite exist = findFavoriteByName(trimmed);
        if (exist != null) {
            return exist.getId();
        }
        JmFavoriteSqlite entity = new JmFavoriteSqlite();
        entity.setName(trimmed);
        entity.setSortOrder(nextFavoriteSortOrder());
        String now = DateTimeUtil.dateTimeFormat(new Date(), DateTimeUtil.PatternEnum.yyyyMMddHHmmss);
        entity.setCreateTime(now);
        entity.setModifyTime(now);
        jmFavoriteSqliteMapper.insert(entity);
        return entity.getId();
    }

    /**
     * 默认收藏夹id。理论上初始化时已建好；这里兜底建一次，避免历史库缺少该记录时报错
     */
    private Long getOrCreateDefaultFavoriteId() {
        JmFavoriteSqlite exist = findFavoriteByName(DEFAULT_FAVORITE_NAME);
        if (exist != null) {
            return exist.getId();
        }
        JmFavoriteSqlite entity = new JmFavoriteSqlite();
        entity.setName(DEFAULT_FAVORITE_NAME);
        entity.setSortOrder(0);
        String now = DateTimeUtil.dateTimeFormat(new Date(), DateTimeUtil.PatternEnum.yyyyMMddHHmmss);
        entity.setCreateTime(now);
        entity.setModifyTime(now);
        jmFavoriteSqliteMapper.insert(entity);
        return entity.getId();
    }

    private JmFavoriteSqlite findFavoriteByName(String name) {
        if (StringUtils.isBlank(name)) {
            return null;
        }
        return jmFavoriteSqliteMapper.selectOne(new LambdaQueryWrapper<JmFavoriteSqlite>()
                .eq(JmFavoriteSqlite::getName, name)
                .last("LIMIT 1"));
    }

    private JmFavoriteSqlite requireFavorite(Long id) {
        if (id == null) {
            throw new BusinessException("缺少收藏夹id");
        }
        JmFavoriteSqlite favorite = jmFavoriteSqliteMapper.selectById(id);
        if (favorite == null) {
            throw new BusinessException("收藏夹不存在或已被删除");
        }
        return favorite;
    }

    private boolean isDefaultFavorite(JmFavoriteSqlite favorite) {
        return favorite != null && DEFAULT_FAVORITE_NAME.equals(favorite.getName());
    }

    private String normalizeFavoriteName(String name) {
        String trimmed = StringUtils.trimToEmpty(name);
        if (StringUtils.isBlank(trimmed)) {
            throw new BusinessException("收藏夹名称不能为空");
        }
        if (trimmed.length() > 50) {
            throw new BusinessException("收藏夹名称不能超过50个字符");
        }
        return trimmed;
    }

    private Integer nextFavoriteSortOrder() {
        JmFavoriteSqlite last = jmFavoriteSqliteMapper.selectOne(new LambdaQueryWrapper<JmFavoriteSqlite>()
                .orderByDesc(JmFavoriteSqlite::getSortOrder)
                .orderByDesc(JmFavoriteSqlite::getId)
                .last("LIMIT 1"));
        int max = last == null || last.getSortOrder() == null ? 0 : last.getSortOrder();
        return max + 1;
    }

    private JmFavoriteResp toFavoriteResp(JmFavoriteSqlite favorite, Long albumCount) {
        JmFavoriteResp resp = new JmFavoriteResp();
        BeanUtils.copyProperties(favorite, resp);
        resp.setAlbumCount(albumCount == null ? 0L : albumCount);
        resp.setIsDefault(isDefaultFavorite(favorite));
        return resp;
    }

    /**
     * 删除所有文件
     * @param request
     */
    @Override
    public void deleteAllFile(JmAlbumDeleteReq request) {
        if (request == null) {
            return;
        }
        File jmcomicDirFile = new File(FileUtil.getJmcomicDir());
        if (!jmcomicDirFile.exists()) {
            throw new BusinessException("JM目录不存在");
        }
        if (!jmcomicDirFile.isDirectory()) {
            throw new BusinessException("JM目录不是文件夹");
        }
        if (Boolean.TRUE.equals(request.getDeleteZip())) {
            this.deleteAllFile(jmcomicDirFile, ".zip");
        }
        if (Boolean.TRUE.equals(request.getDeletePdf())) {
            this.deleteAllFile(jmcomicDirFile, ".pdf");
        }
        if (Boolean.TRUE.equals(request.getDeleteImages())) {
            this.deleteAllImages(jmcomicDirFile);
        }
    }

    private void deleteAllImages(File jmcomicDirFile){
        // 获取jm目录下所有的漫画文件夹
        File[] directoryList = FileUtil.getDirectoryList(jmcomicDirFile);
        if (directoryList == null) {
            return;
        }
        // zip和pdf在漫画文件夹外部 所以不会误删
        for (File file : directoryList) {
            cn.hutool.core.io.FileUtil.del(file);
        }
    }

    private void deleteAllFile(File jmcomicDirFile, String suffix) {
        File[] files = FileUtil.getFileList(jmcomicDirFile, suffix);
        if (files == null) {
            return;
        }
        for (File file : files) {
            file.delete();
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteChapterImages(JmChapterImageDeleteReq request) {
        if (request == null || CollectionUtils.isEmpty(request.getIds())) {
            return;
        }
        List<JmChapterImageSqlite> images = jmChapterImageSqliteMapper.selectByIds(request.getIds());
        if (Boolean.TRUE.equals(request.getDeleteFile())) {
            List<Long> albumIds = images.stream().map(JmChapterImageSqlite::getAlbumId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());
            Map<Long, JmAlbumSqlite> albumMap = CollectionUtils.isEmpty(albumIds) ? Collections.emptyMap()
                    : jmAlbumSqliteMapper.selectByIds(albumIds).stream().collect(Collectors.toMap(JmAlbumSqlite::getId, e -> e));
            images.forEach(e -> deleteFileUnderJmcomic(getImageFile(albumMap.get(e.getAlbumId()), e)));
        }
        if (Boolean.TRUE.equals(request.getDeleteData())) {
            jmChapterImageSqliteMapper.deleteByIds(request.getIds());
        }
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
            entity.setChapterName(series == null ? chapter.getName() : series.getName());
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

    private JmAlbumManageResp toAlbumManageResp(JmAlbumSqlite album) {
        JmAlbumManageResp resp = new JmAlbumManageResp();
        BeanUtils.copyProperties(album, resp);
        File zipFile = getZipFile(album);
        File pdfFile = getPdfFile(album);
        resp.setZipExists(zipFile.exists());
        resp.setPdfExists(pdfFile.exists());
        resp.setServerZipUrl(zipFile.exists() ? buildServerFileUrl(album.getAlbumFolderName() + ".zip") : null);
        resp.setServerPdfUrl(pdfFile.exists() ? buildServerFileUrl(album.getAlbumFolderName() + ".pdf") : null);
        resp.setCoverUrl(JmcomicService.buildCoverUrl(album.getId(), null));
        resp.setServerCoverUrl(coverFileExists(album) ? buildServerCoverUrl(album) : null);
        resp.setChapterList(this.listChapters(album));
        resp.setImageCount(jmChapterImageSqliteMapper.selectCount(new LambdaQueryWrapper<JmChapterImageSqlite>()
                .eq(JmChapterImageSqlite::getAlbumId, album.getId())));
        resp.setActualImageCount(countActualImages(album));
        return resp;
    }

    private JmChapterImageManageResp toChapterImageManageResp(JmChapterImageSqlite image, JmAlbumSqlite album) {
        JmChapterImageManageResp resp = new JmChapterImageManageResp();
        BeanUtils.copyProperties(image, resp);
        resp.setImgUrl(JmcomicService.buildImgUrl(image.getChapterId(), image.getImageFile()));
        resp.setImageFileExists(album != null && getImageFile(album, image).exists());
        resp.setServerImgUrl(buildServerImgUrl(album, image));
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

    private Series findSeries(Long albumId, Long chapterId) {
        JmAlbumSqlite album = jmAlbumSqliteMapper.selectById(albumId);
        if (album == null || StringUtils.isBlank(album.getSeries())) {
            return null;
        }
        List<Series> series = JSONObject.parseObject(album.getSeries(), new TypeReference<List<Series>>() {});
        if (CollectionUtils.isEmpty(series)) {
            return null;
        }
        return series.stream()
                .filter(e -> String.valueOf(chapterId).equals(e.getId()))
                .findFirst()
                .orElse(null);
    }

    private String toJson(Object value) {
        if (Objects.isNull(value)) {
            return null;
        }
        return JSONObject.toJSONString(value);
    }

    private File getZipFile(JmAlbumSqlite album) {
        return new File(FileUtil.getJmcomicDir() + File.separator + album.getAlbumFolderName() + ".zip");
    }

    private File getPdfFile(JmAlbumSqlite album) {
        return new File(FileUtil.getJmcomicDir() + File.separator + album.getAlbumFolderName() + ".pdf");
    }

    private File getAlbumDir(JmAlbumSqlite album) {
        return new File(FileUtil.getJmcomicDir() + File.separator + album.getAlbumFolderName());
    }

    private File getImageFile(JmAlbumSqlite album, JmChapterImageSqlite image) {
        if (album == null || StringUtils.isBlank(album.getAlbumFolderName()) || image == null) {
            return new File("__jmcomic_image_not_exists__");
        }
        return new File(getAlbumDir(album) + File.separator + getChapterFolderName(image) + File.separator + image.getImageFile());
    }

    private String buildServerImgUrl(JmAlbumSqlite album, JmChapterImageSqlite image) {
        if (album == null || StringUtils.isBlank(album.getAlbumFolderName()) || image == null || StringUtils.isBlank(image.getImageFile())) {
            return null;
        }
        return webResourceConfig.webResourcesJmcomicPathInClasses()
                + "/" + urlEncode(album.getAlbumFolderName())
                + "/" + urlEncode(getChapterFolderName(image))
                + "/" + urlEncode(image.getImageFile());
    }

    private String buildServerFileUrl(String fileName) {
        return webResourceConfig.webResourcesJmcomicPathInClasses() + "/" + urlEncode(fileName);
    }

    /**
     * 本地封面文件，与 JmcomicService#downloadCoverImage 落盘位置一致：jmcomic/{本子文件夹}/{jmId}.jpg
     */
    private File getCoverFile(JmAlbumSqlite album) {
        if (album == null || album.getId() == null || StringUtils.isBlank(album.getAlbumFolderName())) {
            return null;
        }
        return new File(getAlbumDir(album) + File.separator + album.getId() + ".jpg");
    }

    private boolean coverFileExists(JmAlbumSqlite album) {
        File coverFile = getCoverFile(album);
        return coverFile != null && coverFile.isFile() && coverFile.length() > 0;
    }

    private String buildServerCoverUrl(JmAlbumSqlite album) {
        if (!coverFileExists(album)) {
            return null;
        }
        return webResourceConfig.webResourcesJmcomicPathInClasses()
                + "/" + urlEncode(album.getAlbumFolderName())
                + "/" + album.getId() + ".jpg";
    }

    private String getChapterFolderName(JmChapterImageSqlite image) {
        String title = StringUtils.isNotBlank(image.getChapterTitle()) ? image.getChapterTitle() : image.getChapterName();
        if (StringUtils.isBlank(title)) {
            title = String.valueOf(image.getChapterId());
        }
        return title;
    }

    private Long countActualImages(JmAlbumSqlite album) {
        File albumDir = getAlbumDir(album);
        if (!albumDir.exists() || !albumDir.isDirectory()) {
            return 0L;
        }
        File[] directoryList = FileUtil.getDirectoryList(albumDir);//获取下面的文件夹（章节目录）
        long count = 0;
        for (File cdir : directoryList) {
            // 累加章节里面的图片数
            count += Stream.of(FileUtil.getFileList(cdir))
                    .filter(file -> IMAGE_EXTENSIONS.contains(StringUtils.defaultString(FileUtil.getFileExtension(file.getName()))))
                    .count();
        }
        return count;
    }

    private void deleteDirectoryQuietly(File directory) {
        if (!directory.exists()) {
            return;
        }
        if (!isUnderJmcomicDir(directory)) {
            log.error("拒绝删除JM漫画目录外的路径：{}", directory.getAbsolutePath());
            return;
        }
        try {
            FileUtils.deleteDirectory(directory);
        } catch (Exception e) {
            log.error("删除JM漫画图片目录异常：{}", directory.getAbsolutePath(), e);
        }
    }

    private void deleteFileUnderJmcomic(File file) {
        if (file == null || !file.exists()) {
            return;
        }
        if (!isUnderJmcomicDir(file)) {
            log.error("拒绝删除JM漫画目录外的文件：{}", file.getAbsolutePath());
            return;
        }
        FileUtil.deleteFile(file);
    }

    private boolean isUnderJmcomicDir(File file) {
        try {
            String rootPath = new File(FileUtil.getJmcomicDir()).getCanonicalPath();
            String filePath = file.getCanonicalPath();
            return filePath.equals(rootPath) || filePath.startsWith(rootPath + File.separator);
        } catch (Exception e) {
            log.error("校验JM漫画文件路径异常：{}", file.getAbsolutePath(), e);
            return false;
        }
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
