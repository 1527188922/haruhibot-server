package com.haruhi.botServer.vo;

import lombok.Data;

@Data
public class JmChapterImageQueryReq extends PageReq {
    private Long albumId;
    private Long chapterId;
    private String chapterTitle;
    private String imageFile;
}
