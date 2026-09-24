package com.haruhi.botserver.features.jmcomic.service;

import com.haruhi.botserver.features.jmcomic.client.model.Album;
import com.haruhi.botserver.features.jmcomic.client.model.Chapter;
import com.haruhi.botserver.features.jmcomic.client.model.Series;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.haruhi.botserver.features.jmcomic.model.JmAlbumDeleteReq;
import com.haruhi.botserver.features.jmcomic.model.JmAlbumCollectReq;
import com.haruhi.botserver.features.jmcomic.model.JmAlbumManageResp;
import com.haruhi.botserver.features.jmcomic.model.JmAlbumQueryReq;
import com.haruhi.botserver.features.jmcomic.model.JmChapterImageDeleteReq;
import com.haruhi.botserver.features.jmcomic.model.JmChapterImageManageResp;
import com.haruhi.botserver.features.jmcomic.model.JmChapterImageQueryReq;
import com.haruhi.botserver.features.jmcomic.model.JmChapterImageResp;
import com.haruhi.botserver.features.jmcomic.model.JmChapterInfoResp;

import java.util.List;

public interface JmcomicSqliteService {

    void saveOrUpdateAlbum(Album album, String raw);

    void saveOrUpdateChapterImages(Long albumId, Chapter chapter);

    void saveOrUpdateChapterImages(Long albumId, Chapter chapter, Series series);

    List<JmChapterInfoResp> listChapters(Long albumId);

    List<JmChapterImageResp> listChapterImages(Long albumId, Long chapterId);

    IPage<JmAlbumManageResp> searchAlbums(JmAlbumQueryReq request);

    List<String> allTag();

    List<String> allAuthor();

    IPage<JmChapterImageManageResp> searchChapterImages(JmChapterImageQueryReq request);

    void deleteAlbums(JmAlbumDeleteReq request);

    void collectAlbums(JmAlbumCollectReq request);

    void deleteAllFile(JmAlbumDeleteReq request);

    void deleteChapterImages(JmChapterImageDeleteReq request);
}
