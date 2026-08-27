package com.haruhi.botServer.service;

import com.haruhi.botServer.dto.jmcomic.Album;
import com.haruhi.botServer.dto.jmcomic.Chapter;
import com.haruhi.botServer.dto.jmcomic.Series;
import com.haruhi.botServer.vo.JmChapterImageResp;
import com.haruhi.botServer.vo.JmChapterInfoResp;

import java.util.List;

public interface JmcomicSqliteService {

    void saveOrUpdateAlbum(Album album, String raw);

    void saveOrUpdateChapterImages(Long albumId, Chapter chapter);

    void saveOrUpdateChapterImages(Long albumId, Chapter chapter, Series series);

    List<JmChapterInfoResp> listChapters(Long albumId);

    List<JmChapterImageResp> listChapterImages(Long albumId, Long chapterId);
}
