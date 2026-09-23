package com.haruhi.botserver.features.jmcomic.model;

import com.haruhi.botserver.shared.model.PageReq;

import lombok.Data;

@Data
public class JmChapterImageQueryReq extends PageReq {
    private Long albumId;
    private Long chapterId;
    private String chapterTitle;
    private String imageFile;
}
