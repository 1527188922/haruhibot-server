package com.haruhi.botServer.dto;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

import java.util.List;

@Data
public class AnalysisMagnetLinkResp {

    private String error;
    private String type;
    @JSONField(name = "file_type")
    private String fileType;
    private String name;
    private Long size;
    private Integer count;

    private List<Screenshots> screenshots;


    @Data
    public static class Screenshots{
        private Integer time;
        private String screenshot;
    }
}
