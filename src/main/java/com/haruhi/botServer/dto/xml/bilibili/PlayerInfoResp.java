package com.haruhi.botServer.dto.xml.bilibili;

import lombok.Data;

import java.io.Serializable;

/**
 * 请求根据bv获取cid接口的响应
 */
@Data
public class PlayerInfoResp implements Serializable {
    private String cid;
    private Integer page;
    private String from;
    private String part;
    private String duration;
    private String vid;
    private String weblink;
    private String first_frame;

}
