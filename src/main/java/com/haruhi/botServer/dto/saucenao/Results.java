package com.haruhi.botServer.dto.saucenao;


import java.io.Serializable;
import java.util.Date;

@lombok.Data
public class Results implements Serializable {
    public Header header;
    private Data data;


    @lombok.Data
    public static class Header implements Serializable {
        private String thumbnail;
        private int hidden;
        private int dupes;
        private Double similarity;
        private int index_id;
        private String index_name;
    }

    @lombok.Data
    public static class Data implements Serializable {
        private Long member_id;
        private String[] ext_urls;
        private String pixiv_id;
        private String title;
        private String member_name;
        private String twitter_user_id;
        private String twitter_user_handle;
        private Date created_at;
        // 出处
        private String material;
        // 漫画结果字段
        private String source;
        private String creator;
        private String eng_name;
        private String jp_name;

    }
}
