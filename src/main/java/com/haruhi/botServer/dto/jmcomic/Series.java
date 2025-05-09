package com.haruhi.botServer.dto.jmcomic;

import lombok.Data;

@Data
public class Series {
    private String id;// chapterId
    private String name;
    private String sort;


    // 非接口响应
    private String title;
}