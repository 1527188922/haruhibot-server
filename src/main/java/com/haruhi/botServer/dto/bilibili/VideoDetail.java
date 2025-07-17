package com.haruhi.botServer.dto.bilibili;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;

@Data
public class VideoDetail {

    private String vVoucher;

    @JSONField(name = "View")
    private View view;


    @JSONField(name = "Tags")
    private List<Tag> tags;

    private List<String> participle;

    private Boolean replaceRecommend;
    private Boolean isHitLabourDayActivity;

    public Long getCidFirst(){
        if (view != null) {
            if (CollectionUtils.isNotEmpty(view.getPages())) {
                return view.getPages().get(0).getCid();
            }
            return view.getCid();
        }
        return null;
    }




    @Data
    public static class View{
        private String bvid;
        private Long aid;
        private Integer videos;
        private Long tid;
        private Long tidV2;
        private String tname;
        private String tnameV2;
        private Integer copyright;
        private String pic;
        private String title;
        private Long pubdate;
        private Long ctime;
        private String desc;

        private Long duration;

        private Owner owner;

        private Stat stat;

        private Long cid;

        private List<Page> pages;


        @Data
        public static class Owner{
            private Long mid;
            private String name;
            private String face;
        }

        @Data
        public static class Stat{
            private Long aid;
            private Long view;
            private Long danmaku;
            private Long reply;
            private Long favorite;
            private Long coin;
            private Long share;
            private Long nowRank;
            private Long hisRank;
            private Long like;
            private Long dislike;
            private String evaluation;
            private Long vt;
        }

        @Data
        public static class Page{
            private Long cid;
            private Integer page;
            private String from;
            private String part;
            private Long duration;
            private String vid;
            private String weblink;
            private String firstFrame;
            private Long ctime;
        }

    }

    @Data
    public static class Tag{
        private Long tagId;
        private String tagName;
        private String musicId;
        private String tagType;
        private String jumpUrl;
    }
}
