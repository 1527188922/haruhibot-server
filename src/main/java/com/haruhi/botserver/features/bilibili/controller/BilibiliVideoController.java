package com.haruhi.botserver.features.bilibili.controller;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.haruhi.botserver.bootstrap.SysConstants;
import com.haruhi.botserver.features.bilibili.model.BilibiliVideoAddReq;
import com.haruhi.botserver.features.bilibili.model.BilibiliVideoIdReq;
import com.haruhi.botserver.features.bilibili.model.BilibiliVideoQueryReq;
import com.haruhi.botserver.features.bilibili.model.BilibiliVideoResp;
import com.haruhi.botserver.features.bilibili.service.BilibiliVideoService;
import com.haruhi.botserver.shared.error.BusinessException;
import com.haruhi.botserver.shared.model.HttpResp;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * bilibili视频管理（webui「BILIBILI视频」菜单）
 */
@Slf4j
@RestController
@RequestMapping(SysConstants.CONTEXT_PATH + "/bilibili/video")
public class BilibiliVideoController {

    @Autowired
    private BilibiliVideoService bilibiliVideoService;

    /**
     * 分页查询视频卡片数据，按发布时间倒序
     * @param request {bvid,title,ownerMid,ownerName,tag,currentPage,pageSize}
     */
    @PostMapping("/search")
    public HttpResp<IPage<BilibiliVideoResp>> search(@RequestBody(required = false) BilibiliVideoQueryReq request) {
        try {
            return HttpResp.success(bilibiliVideoService.search(request));
        } catch (Exception e) {
            log.error("[webui][/bilibili/video]查询视频异常：{}", JSONObject.toJSONString(request), e);
            return HttpResp.fail("查询异常：" + e.getMessage(), null);
        }
    }

    /**
     * 按bv号添加视频：输入文本中解析bv号后调用详情接口入库
     */
    @PostMapping("/add")
    public HttpResp<BilibiliVideoResp> add(@RequestBody BilibiliVideoAddReq request) {
        if (Objects.isNull(request) || Objects.isNull(request.getText()) || request.getText().isBlank()) {
            return HttpResp.fail("请输入b站视频链接或bv号", null);
        }
        try {
            return HttpResp.success("添加成功", bilibiliVideoService.addByText(request.getText()));
        } catch (BusinessException e) {
            return HttpResp.fail(e.getMessage(), null);
        } catch (Exception e) {
            log.error("[webui][/bilibili/video]添加视频异常：{}", JSONObject.toJSONString(request), e);
            return HttpResp.fail("添加异常：" + e.getMessage(), null);
        }
    }

    /**
     * 重新拉取视频详情
     */
    @PostMapping("/refresh")
    public HttpResp<BilibiliVideoResp> refresh(@RequestBody BilibiliVideoIdReq request) {
        if (Objects.isNull(request) || Objects.isNull(request.getId())) {
            return HttpResp.fail("缺少视频记录id", null);
        }
        try {
            return HttpResp.success("刷新成功", bilibiliVideoService.refresh(request.getId()));
        } catch (BusinessException e) {
            return HttpResp.fail(e.getMessage(), null);
        } catch (Exception e) {
            log.error("[webui][/bilibili/video]刷新视频异常：{}", JSONObject.toJSONString(request), e);
            return HttpResp.fail("刷新异常：" + e.getMessage(), null);
        }
    }

    /**
     * 下载视频到本地，异步执行，用 /download/status 查询进度
     */
    @PostMapping("/download")
    public HttpResp<BilibiliVideoService.DownloadState> download(@RequestBody BilibiliVideoIdReq request) {
        if (Objects.isNull(request) || Objects.isNull(request.getId())) {
            return HttpResp.fail("缺少视频记录id", null);
        }
        try {
            return HttpResp.success("已开始下载", bilibiliVideoService.startDownload(request.getId()));
        } catch (BusinessException e) {
            return HttpResp.fail(e.getMessage(), null);
        } catch (Exception e) {
            log.error("[webui][/bilibili/video]下载视频异常：{}", JSONObject.toJSONString(request), e);
            return HttpResp.fail("下载异常：" + e.getMessage(), null);
        }
    }

    /**
     * 查询视频下载状态，key: 视频记录id
     */
    @PostMapping("/download/status")
    public HttpResp<Map<Long, BilibiliVideoService.DownloadState>> downloadStatus(@RequestBody(required = false) BilibiliVideoIdReq request) {
        List<Long> ids = Objects.isNull(request) ? null : request.getIds();
        return HttpResp.success(bilibiliVideoService.downloadStates(ids));
    }

    /**
     * 批量删除记录（本地已下载的视频文件保留）
     */
    @PostMapping("/deleteBatch")
    public HttpResp deleteBatch(@RequestBody BilibiliVideoIdReq request) {
        if (Objects.isNull(request) || CollectionUtils.isEmpty(request.getIds())) {
            return HttpResp.fail("请选择要删除的视频", null);
        }
        try {
            int count = bilibiliVideoService.deleteBatch(request.getIds());
            return HttpResp.success("已删除" + count + "条视频记录", null);
        } catch (Exception e) {
            log.error("[webui][/bilibili/video]删除视频异常：{}", JSONObject.toJSONString(request), e);
            return HttpResp.fail("删除异常：" + e.getMessage(), null);
        }
    }
}
