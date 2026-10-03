package com.haruhi.botserver.features.bilibili.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.haruhi.botserver.infrastructure.persistence.DataBaseConst;
import lombok.Data;

/**
 * b站视频信息
 * <p>
 * 数据来源：{@code https://api.bilibili.com/x/web-interface/wbi/view/detail}
 * （{@link com.haruhi.botserver.features.bilibili.service.BilibiliService#getVideoDetail(String)}），
 * 每次调用都会按 bvid+cid 更新或插入，bvid+cid 组合唯一。
 * <p>
 * 播放地址（play_url_raw）来自
 * {@link com.haruhi.botserver.features.bilibili.service.BilibiliService#getPlayUrlInfo(String, Long, Long)}，
 * 只有调用过该接口才会写入，getVideoDetail 的更新不会清空它。
 */
@Data
@TableName(value = DataBaseConst.T_BILIBILI_VIDEO)
public class BilibiliVideoSqlite {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * b站视频bv号
     */
    private String bvid;

    /**
     * b站视频av号
     */
    private Long avid;

    /**
     * b站视频cid
     */
    private Long cid;

    private String title;

    /**
     * 视频标签，多个用逗号分隔
     */
    private String tag;

    /**
     * 视频简介
     * <p>
     * desc 是 sql 关键字，建表与查询都带反引号
     */
    @TableField(value = "`desc`")
    private String desc;

    /**
     * 视频封面url
     */
    private String pic;

    /**
     * 视频时长，单位：秒
     */
    private Long duration;

    /**
     * up主uid
     */
    private Long ownerMid;

    /**
     * up主昵称
     */
    private String ownerName;

    /**
     * up主头像url
     */
    private String ownerFace;

    /**
     * getVideoDetail 接口响应的原始json
     */
    private String videoDetailRaw;

    /**
     * getPlayUrlInfo 接口响应的原始json，未调用过该接口时为空
     */
    private String playUrlRaw;

    /**
     * 视频发布时间，unix秒
     */
    private Long pubdate;

    /**
     * 视频创建时间(上传到b站的时间)，unix秒
     */
    private Long ctime;

    private String createTime;

    private String updateTime;
}
