package com.haruhi.botServer.vo;

import lombok.Data;

@Data
public class JmChapterInfoResp {
    private Long albumId;
    private Long chapterId;
    private String name;
    private String sort;
    private String title;
}
