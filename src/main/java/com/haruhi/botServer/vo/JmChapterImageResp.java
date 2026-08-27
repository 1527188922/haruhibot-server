package com.haruhi.botServer.vo;

import lombok.Data;

@Data
public class JmChapterImageResp {
    private Long albumId;
    private Long chapterId;
    private String chapterName;
    private String chapterAddTime;
    private String seriesId;
    private String imageFile;
    private Integer imageSort;
    private String imgUrl;
}
