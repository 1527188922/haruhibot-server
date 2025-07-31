package com.haruhi.botServer.dto.jmcomic;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
public class Album {

    private Long id;
    private String name;
    private String albumFolderName;// 本子文件夹名称 非接口响应字段
    private List<Object> images;
    private String addTime;
    private String description;
    private String totalViews;
    private String likes;
    private List<Series> series;
    private String seriesId;
    private String commentTotal;
    private List<String> author;
    private List<String> tags;
    private List<String> works;
    private List<String> actors;
    private List<RelatedItem> relatedList;
    private Boolean liked;
    private Boolean isFavorite;
    private Boolean isAids;
    private String price;
    private String purchased;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RelatedItem {
        private String id;
        private String author;
        private String name;
        private String image;
    }
}
