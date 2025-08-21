package com.haruhi.botServer.dto.bilibili;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

import java.io.Serializable;

/**
 * 请求根据bv获取cid接口的响应
 */
@Data
public class PlayerInfoResp implements Serializable {
    private Long cid;
    private Integer page;
    private String from;
    private String part;
    private Long duration;
    private String vid;
    private String weblink;
    @JSONField(name = "first_frame")
    private String firstFrame;
    private Long ctime;
    private Dimension dimension;

    @Data
    public static class Dimension{
        private Long width;
        private Long height;
        private Double rotate;
    }

}
