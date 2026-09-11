package com.haruhi.botServer.dto.bilibili;

import com.alibaba.fastjson.annotation.JSONField;
import cn.hutool.core.text.StrFormatter;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;

/**
 * b站直播间状态
 * 接口：https://api.live.bilibili.com/room/v1/Room/get_status_info_by_uids
 */
@Data
public class LiveStatusInfo implements Serializable {

    /**
     * 未开播
     */
    public static final int STATUS_OFF = 0;
    /**
     * 直播中
     */
    public static final int STATUS_LIVE = 1;
    /**
     * 轮播中（轮播不算开播）
     */
    public static final int STATUS_ROUND = 2;

    public static final String LIVE_URL_TEMPLATE = "https://live.bilibili.com/{}";
    private String title;
    @JSONField(name = "room_id")
    private Long roomId;
    private Long uid;
    private Long online;
    /**
     * 开播时间 单位秒
     */
    @JSONField(name = "live_time")
    private Long liveTime;
    @JSONField(name = "live_status")
    private Integer liveStatus;
    @JSONField(name = "short_id")
    private Long shortId;
    private Integer area;
    @JSONField(name = "area_name")
    private String areaName;
    @JSONField(name = "area_v2_id")
    private Integer areaV2Id;
    @JSONField(name = "area_v2_name")
    private String areaV2Name;
    @JSONField(name = "area_v2_parent_name")
    private String areaV2ParentName;
    @JSONField(name = "area_v2_parent_id")
    private Integer areaV2ParentId;
    private String uname;
    private String face;
    @JSONField(name = "tag_name")
    private String tagName;
    private String tags;
    @JSONField(name = "cover_from_user")
    private String coverFromUser;
    private String keyframe;

    /**
     * 是否正在直播，轮播不算
     */
    public boolean isLiving() {
        return liveStatus != null && liveStatus == STATUS_LIVE;
    }

    /**
     * 直播间地址，优先使用短号
     */
    public String roomUrl() {
        Long id = shortId != null && shortId > 0 ? shortId : roomId;
        return id == null ? null : StrFormatter.format(LIVE_URL_TEMPLATE, id);
    }

    /**
     * 直播间封面，优先使用主播自己上传的封面
     */
    public String cover() {
        return StringUtils.isNotBlank(coverFromUser) ? coverFromUser : keyframe;
    }

    /**
     * 直播间分区，如：单机游戏 / 其他单机
     */
    public String areaFullName() {
        if (StringUtils.isBlank(areaV2ParentName)) {
            return StringUtils.defaultString(areaV2Name);
        }
        return areaV2ParentName + " / " + StringUtils.defaultString(areaV2Name);
    }
}
