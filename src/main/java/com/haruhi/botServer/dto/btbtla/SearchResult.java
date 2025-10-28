package com.haruhi.botServer.dto.btbtla;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
public class SearchResult {

    private String keyword;
    private List<ModuleItem> moduleItems;

    private Exception exception;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ModuleItem{
        private String title;
        private String description;
        private String itemPicUrl;
        private String detailHref;
        private String year;
        private String country;
        private String category;

    }

}
