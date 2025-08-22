package com.haruhi.botServer.dto.whatslink;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

import java.util.List;

/**
 * 预览磁力接口响应
 */
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
