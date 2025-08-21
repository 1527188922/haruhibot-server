package com.haruhi.botServer.dto.jmcomic;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

import java.util.List;

@Data
public class SearchResp {

    @JSONField(name = "search_query")
    private String searchQuery;
    private String total;
    private List<ContentItem> content;


    @Data
    public static class ContentItem{
        private String id;
        private String author;
        private String description;
        private String name;
        private String image;
        private Category category;
        @JSONField(name = "category_sub")
        private Category categorySub;
        private Boolean liked;
        @JSONField(name = "is_favorite")
        private Boolean isFavorite;
        @JSONField(name = "update_at")
        private Long updateAt;//10位时间戳

        @Data
        public static class Category{
            private String id;
            private String title;
        }
    }

}
