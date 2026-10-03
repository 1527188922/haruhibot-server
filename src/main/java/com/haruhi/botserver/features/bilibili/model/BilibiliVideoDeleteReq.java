package com.haruhi.botserver.features.bilibili.model;

import lombok.Data;

import java.util.List;

/**
 * b站视频删除请求
 * <p>
 * {@link #deleteData} 与 {@link #deleteFile} 是两个独立的勾选项，勾了哪个删哪个（可以都勾）：
 * 只勾数据库记录时本地视频文件保留（与以前的行为一致）。
 */
@Data
public class BilibiliVideoDeleteReq {

    private List<Long> ids;

    /**
     * 是否删除 t_bilibili_video 记录
     */
    private Boolean deleteData;

    /**
     * 是否删除 video/bilibili 下的本地视频文件
     */
    private Boolean deleteFile;
}
