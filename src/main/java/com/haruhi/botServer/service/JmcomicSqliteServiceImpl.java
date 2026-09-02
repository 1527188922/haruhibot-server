package com.haruhi.botServer.service;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.haruhi.botServer.config.webResource.AbstractWebResourceConfig;
import com.haruhi.botServer.dto.jmcomic.Album;
import com.haruhi.botServer.dto.jmcomic.Chapter;
import com.haruhi.botServer.dto.jmcomic.Series;
import com.haruhi.botServer.entity.JmAlbumSqlite;
import com.haruhi.botServer.entity.JmChapterImageSqlite;
import com.haruhi.botServer.mapper.JmAlbumSqliteMapper;
import com.haruhi.botServer.mapper.JmChapterImageSqliteMapper;
import com.haruhi.botServer.utils.DateTimeUtil;
import com.haruhi.botServer.utils.FileUtil;
import com.haruhi.botServer.vo.JmAlbumDeleteReq;
import com.haruhi.botServer.vo.JmAlbumManageResp;
import com.haruhi.botServer.vo.JmAlbumQueryReq;
import com.haruhi.botServer.vo.JmChapterImageDeleteReq;
import com.haruhi.botServer.vo.JmChapterImageManageResp;
import com.haruhi.botServer.vo.JmChapterImageQueryReq;
import com.haruhi.botServer.vo.JmChapterImageResp;
import com.haruhi.botServer.vo.JmChapterInfoResp;
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
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@Slf4j
public class JmcomicSqliteServiceImpl implements JmcomicSqliteService {

    private static final Set<String> IMAGE_EXTENSIONS = new HashSet<>(Arrays.asList("jpg", "jpeg", "png", "gif", "webp", "bmp"));

    @Autowired
    private JmAlbumSqliteMapper jmAlbumSqliteMapper;

    @Autowired
    private JmChapterImageSqliteMapper jmChapterImageSqliteMapper;

    @Autowired
    private AbstractWebResourceConfig webResourceConfig;

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
                .like(StringUtils.isNotBlank(request.getTags()), JmAlbumSqlite::getTags, request.getTags())
                .orderByDesc(JmAlbumSqlite::getModifyTime)
                .orderByDesc(JmAlbumSqlite::getId);
        IPage<JmAlbumSqlite> sourcePage = jmAlbumSqliteMapper.selectPage(new Page<>(request.getCurrentPage(), request.getPageSize()), queryWrapper);
        Page<JmAlbumManageResp> targetPage = new Page<>(sourcePage.getCurrent(), sourcePage.getSize(), sourcePage.getTotal());
        List<JmAlbumManageResp> records = sourcePage.getRecords().stream()
                .map(this::toAlbumManageResp)
                .collect(Collectors.toList());
        targetPage.setRecords(records);
        return targetPage;
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
        List<JmAlbumSqlite> albums = jmAlbumSqliteMapper.selectBatchIds(request.getIds());
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
        jmChapterImageSqliteMapper.delete(new LambdaQueryWrapper<JmChapterImageSqlite>()
                .in(JmChapterImageSqlite::getAlbumId, request.getIds()));
        jmAlbumSqliteMapper.deleteBatchIds(request.getIds());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteChapterImages(JmChapterImageDeleteReq request) {
        if (request == null || CollectionUtils.isEmpty(request.getIds())) {
            return;
        }
        List<JmChapterImageSqlite> images = jmChapterImageSqliteMapper.selectBatchIds(request.getIds());
        if (Boolean.TRUE.equals(request.getDeleteFile())) {
            List<Long> albumIds = images.stream().map(JmChapterImageSqlite::getAlbumId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());
            Map<Long, JmAlbumSqlite> albumMap = CollectionUtils.isEmpty(albumIds) ? Collections.emptyMap()
                    : jmAlbumSqliteMapper.selectBatchIds(albumIds).stream().collect(Collectors.toMap(JmAlbumSqlite::getId, e -> e));
            images.forEach(e -> deleteFileUnderJmcomic(getImageFile(albumMap.get(e.getAlbumId()), e)));
        }
        jmChapterImageSqliteMapper.deleteBatchIds(request.getIds());
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

    private String getChapterFolderName(JmChapterImageSqlite image) {
        String title = StringUtils.isNotBlank(image.getChapterTitle()) ? image.getChapterTitle() : image.getChapterName();
        if (StringUtils.isBlank(title)) {
            title = String.valueOf(image.getChapterId());
        }
        return title + (StringUtils.isBlank(image.getChapterName()) ? "" : "_" + image.getChapterName());
    }

    private Long countActualImages(JmAlbumSqlite album) {
        File albumDir = getAlbumDir(album);
        if (!albumDir.exists() || !albumDir.isDirectory()) {
            return 0L;
        }
        return FileUtil.getAllFiles(albumDir.getAbsolutePath()).stream()
                .filter(file -> IMAGE_EXTENSIONS.contains(StringUtils.defaultString(FileUtil.getFileExtension(file.getName()))))
                .count();
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
