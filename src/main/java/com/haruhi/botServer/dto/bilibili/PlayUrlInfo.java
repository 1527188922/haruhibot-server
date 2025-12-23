package com.haruhi.botServer.dto.bilibili;

import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;

@Data
public class PlayUrlInfo {


    private String from;
    private String result;

    private String message;
    private Integer quality;
    private List<Integer> acceptQuality;
    private String format;
    private Long timelength;
    private String acceptFormat;

    private List<String> acceptDescription;
    private Integer videoCodecid;
    private String seekParam;
    private String seekType;
    private List<Durl> durl;

    private List<SupportFormat> supportFormats;

    private String highFormat;

    public String getDurlFirst(){
        if (CollectionUtils.isNotEmpty(durl)) {
            return durl.getFirst().getUrl();
        }
        return null;
    }


    @Data
    public static class Durl {
        private Integer order;
        private Long length;
        private Long size;
        private String ahead;
        private String vhead;
        private String url;
        private List<String> backupUrl;
    }

    @Data
    public static class SupportFormat {
        private Integer quality;
        private String format;
        private String newDescription;
        private String displayDesc;
        private String superscript;

    }




}
