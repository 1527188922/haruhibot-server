package com.haruhi.botServer.dto.music163;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class SearchResp implements Serializable {

    private String id;
    private String name;
    private List<AR> ar;
    private AL al;
    // 翻译
    private List<String> tns;

    /**
     * 歌手信息
     */
    @Data
    public class AR implements Serializable{
        private String id;
        private String name;
    }

    /**
     * 专辑信息
     */
    @Data
    public class AL implements Serializable{
        private String id;
        // 专辑名称
        private String name;
        // 专辑封面
        private String picUrl;
    }
}
