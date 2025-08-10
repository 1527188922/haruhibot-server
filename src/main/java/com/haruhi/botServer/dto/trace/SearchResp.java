package com.haruhi.botServer.dto.trace;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
public class SearchResp<T> {
    private Long frameCount;
    private String error;
    private List<Result<T>> result;


    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Result<T> {
        private T anilist;
        private String filename;
        private Long episode;
        private Float from;
        private Float to;
        private Float at;
        private Float duration;
        private Float similarity;
        private String video;
        private String image;
    }
}
