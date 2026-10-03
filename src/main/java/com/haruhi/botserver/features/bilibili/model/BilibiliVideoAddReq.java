package com.haruhi.botserver.features.bilibili.model;

import lombok.Data;

/**
 * 按bv号添加视频的请求参数
 */
@Data
public class BilibiliVideoAddReq {

    /**
     * 含bv号的文本（bv号/av号/b站链接/短链接均可）
     */
    private String text;
}
