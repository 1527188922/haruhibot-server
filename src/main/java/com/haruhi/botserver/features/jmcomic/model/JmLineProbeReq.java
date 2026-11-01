package com.haruhi.botserver.features.jmcomic.model;

import lombok.Data;

import java.util.List;

/**
 * 线路测速请求（配置管理页「测速」按钮）。
 */
@Data
public class JmLineProbeReq {

    /**
     * 测速类型：{@code JM_API}（接口线路）或 {@code JM_IMAGE}（图片线路）。
     * <p>
     * 取值就是配置项控件元数据里的 {@code control.probe}，前端原样回传。
     */
    private String kind;

    /**
     * 额外要测的域名：用户手填、清单里没有的当前值。
     * 可以不传；重复或空白会被忽略。
     */
    private List<String> hosts;
}
