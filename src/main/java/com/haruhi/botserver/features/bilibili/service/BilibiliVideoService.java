package com.haruhi.botserver.features.bilibili.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.haruhi.botserver.features.bilibili.client.model.bilibili.BilibiliBaseResp;
import com.haruhi.botserver.features.bilibili.client.model.bilibili.VideoDetail;
import com.haruhi.botserver.features.bilibili.model.BilibiliVideoAuthorResp;
import com.haruhi.botserver.features.bilibili.model.BilibiliVideoDownloadSnapshot;
import com.haruhi.botserver.features.bilibili.model.BilibiliVideoDownloadTask;
import com.haruhi.botserver.features.bilibili.model.BilibiliVideoQueryReq;
import com.haruhi.botserver.features.bilibili.model.BilibiliVideoResp;
import com.haruhi.botserver.features.bilibili.model.BilibiliVideoTagResp;
import com.haruhi.botserver.features.bilibili.persistence.entity.BilibiliVideoSqlite;
import com.haruhi.botserver.shared.error.BusinessException;
import com.haruhi.botserver.shared.util.FileUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * b站视频管理（webui菜单用）
 * <p>
 * 数据入库由 {@link BilibiliService#getVideoDetail(String)} 自动完成，
 * 这里只做查询、按bv号添加、分组列表、下载编排等。
 */
@Service
@Slf4j
public class BilibiliVideoService {

    @Autowired
    private BilibiliService bilibiliService;

    @Autowired
    private BilibiliVideoSqliteService bilibiliVideoSqliteService;

    @Autowired
    private BilibiliVideoDownloadService bilibiliVideoDownloadService;

    /**
     * 分页查询，按发布时间倒序
     */
    public IPage<BilibiliVideoResp> search(BilibiliVideoQueryReq request) {
        BilibiliVideoQueryReq req = Objects.isNull(request) ? new BilibiliVideoQueryReq() : request;
        LambdaQueryWrapper<BilibiliVideoSqlite> queryWrapper = new LambdaQueryWrapper<BilibiliVideoSqlite>()
                .like(StringUtils.isNotBlank(req.getBvid()), BilibiliVideoSqlite::getBvid, req.getBvid())
                .like(StringUtils.isNotBlank(req.getTitle()), BilibiliVideoSqlite::getTitle, req.getTitle())
                .eq(Objects.nonNull(req.getOwnerMid()), BilibiliVideoSqlite::getOwnerMid, req.getOwnerMid())
                .like(StringUtils.isNotBlank(req.getOwnerName()), BilibiliVideoSqlite::getOwnerName, req.getOwnerName())
                .like(StringUtils.isNotBlank(req.getTag()), BilibiliVideoSqlite::getTag, req.getTag())
                .orderByDesc(BilibiliVideoSqlite::getPubdate)
                .orderByDesc(BilibiliVideoSqlite::getId);
        // 标签是逗号分隔保存的，instr 可以精确匹配"完整的一个标签"，不会被"财经商业"命中"财经"
        if (StringUtils.isNotBlank(req.getTagExact())) {
            queryWrapper.apply("instr(',' || tag || ',', ',' || {0} || ',') > 0", req.getTagExact());
        }
        IPage<BilibiliVideoSqlite> sourcePage = bilibiliVideoSqliteService.page(
                new Page<>(req.getCurrentPage(), req.getPageSize()), queryWrapper);

        Page<BilibiliVideoResp> targetPage = new Page<>(sourcePage.getCurrent(), sourcePage.getSize(), sourcePage.getTotal());
        targetPage.setRecords(sourcePage.getRecords().stream().map(this::toResp).toList());
        return targetPage;
    }

    /**
     * 所有视频作者（附带视频数），webui左侧分组列表
     */
    public List<BilibiliVideoAuthorResp> listAuthors() {
        return bilibiliVideoSqliteService.listAuthors();
    }

    /**
     * 所有标签（附带视频数），webui左侧分组列表
     */
    public List<BilibiliVideoTagResp> listTags() {
        return bilibiliVideoSqliteService.listTags();
    }

    /**
     * 按bv号添加视频：解析文本中的bv号后调用详情接口，详情接口内部会自动入库
     * @return 入库后的视频
     */
    public BilibiliVideoResp addByText(String text) {
        String bvid = bilibiliService.getBvidInText(StringUtils.trimToEmpty(text));
        if (StringUtils.isBlank(bvid)) {
            throw new BusinessException("未能从输入内容中解析出b站视频bv号");
        }
        BilibiliVideoResp video = refreshByBvid(bvid);
        if (Objects.isNull(video)) {
            throw new BusinessException("未查询到视频信息，请确认bv号是否正确：" + bvid);
        }
        return video;
    }

    /**
     * 重新拉取视频详情（详情接口内部会更新入库），失败返回null
     */
    public BilibiliVideoResp refreshByBvid(String bvid) {
        BilibiliBaseResp<VideoDetail> resp = bilibiliService.getVideoDetail(bvid);
        if (Objects.isNull(resp) || Objects.isNull(resp.getData()) || Objects.isNull(resp.getData().getView())) {
            return null;
        }
        VideoDetail detail = resp.getData();
        Long cid = detail.getCidFirst();
        BilibiliVideoSqlite entity = bilibiliVideoSqliteService.getByBvidAndCid(detail.getView().getBvid(), cid);
        return Objects.isNull(entity) ? null : toResp(entity);
    }

    /**
     * 按记录id重新拉取详情
     */
    public BilibiliVideoResp refresh(Long id) {
        BilibiliVideoSqlite entity = bilibiliVideoSqliteService.getById(id);
        if (Objects.isNull(entity)) {
            throw new BusinessException("视频记录不存在");
        }
        BilibiliVideoResp resp = refreshByBvid(entity.getBvid());
        if (Objects.isNull(resp)) {
            throw new BusinessException("拉取视频详情失败：" + entity.getBvid());
        }
        return resp;
    }

    /**
     * 批量删除记录（只删数据库记录，本地已下载的视频文件保留）
     */
    public int deleteBatch(List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return 0;
        }
        List<Long> distinctIds = ids.stream().filter(Objects::nonNull).distinct().toList();
        if (distinctIds.isEmpty()) {
            return 0;
        }
        bilibiliVideoSqliteService.removeByIds(distinctIds);
        return distinctIds.size();
    }

    /**
     * 开始下载视频：同一个 bvid+cid 只会有一个进行中的任务，
     * 进度由 {@link BilibiliVideoDownloadPushService} 通过WebSocket推送
     */
    public BilibiliVideoDownloadTask startDownload(Long id) {
        BilibiliVideoSqlite entity = bilibiliVideoSqliteService.getById(id);
        if (Objects.isNull(entity)) {
            throw new BusinessException("视频记录不存在");
        }
        return bilibiliVideoDownloadService.start(entity);
    }

    /**
     * 下载任务快照，供页面轮询兜底（WebSocket未连通时）
     */
    public BilibiliVideoDownloadSnapshot downloadSnapshot() {
        return bilibiliVideoDownloadService.snapshot();
    }

    /**
     * 实体转卡片数据
     */
    public BilibiliVideoResp toResp(BilibiliVideoSqlite entity) {
        BilibiliVideoResp resp = new BilibiliVideoResp();
        BeanUtils.copyProperties(entity, resp);
        resp.setHasPlayUrl(StringUtils.isNotBlank(entity.getPlayUrlRaw()));
        String fileName = bilibiliVideoDownloadService.videoFile(entity).getName();
        resp.setVideoFileName(fileName);
        resp.setVideoPath(videoPath(fileName));
        resp.setDownloaded(bilibiliVideoDownloadService.isDownloaded(entity));

        BilibiliVideoDownloadTask task = bilibiliVideoDownloadService.lastTask(entity.getBvid(), entity.getCid());
        if (Objects.isNull(task)) {
            resp.setDownloading(false);
        } else {
            resp.setDownloading(task.isRunning());
            resp.setDownloadState(task.getStatus());
            resp.setDownloadMessage(task.getMessage());
            resp.setDownloadPercent(task.getPercent());
            resp.setDownloadSpeed(task.getSpeed());
        }
        return resp;
    }

    /**
     * 服务器本地视频的访问路径。
     * <p>
     * 用相对路径而不是 {@link com.haruhi.botserver.bootstrap.WebResourceConfig#webVideoBiliPath()}，
     * 后者是配置里的对外host，管理员从别的地址访问webui时该host不一定可达；
     * 相对路径由浏览器按当前站点补全，开发环境(8091)的代理也已经覆盖 /video。
     */
    public String videoPath(String fileName) {
        return "/" + FileUtil.DIR_VIDEO + "/" + FileUtil.DIR_VIDEO_BILIBILI + "/" + fileName;
    }
}
