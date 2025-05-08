package com.haruhi.botServer.dto.jmcomic;

import lombok.Data;

import java.util.List;

@Data
public class Chapter {
    private Long id;
    private List<Series> series;
    private String tags;
    private String name;
    private List<String> images;
    private String addTime;
    private String seriesId;
    private Boolean isFavorite;
    private Boolean liked;

}
