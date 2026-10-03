package com.haruhi.botserver.features.bilibili.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.haruhi.botserver.features.bilibili.client.model.bilibili.BilibiliBaseResp;
import com.haruhi.botserver.features.bilibili.client.model.bilibili.PlayUrlInfo;
import com.haruhi.botserver.features.bilibili.client.model.bilibili.VideoDetail;
import com.haruhi.botserver.features.bilibili.model.BilibiliVideoQueryReq;
import com.haruhi.botserver.features.bilibili.model.BilibiliVideoResp;
import com.haruhi.botserver.features.bilibili.persistence.entity.BilibiliVideoSqlite;
import com.haruhi.botserver.infrastructure.concurrent.ThreadPoolUtil;
import com.haruhi.botserver.infrastructure.logging.DbLog;
import com.haruhi.botserver.shared.constant.BusinessModuleEnum;
import com.haruhi.botserver.shared.error.BusinessException;
import com.haruhi.botserver.shared.util.FileUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * b站视频管理（webui菜单用）
 * <p>
 * 数据入库由 {@link BilibiliService#getVideoDetail(String)} 自动完成，
 * 这里只做查询、按bv号添加、下载到本地等编排。
 */
@Service
@Slf4j
public class BilibiliVideoService {

    /**
     * 本地视频文件后缀，与 {@link com.haruhi.botserver.features.bilibili.handler.BilibiliVideoParseHandler} 保持一致
     */
    private static final String VIDEO_SUFFIX = "mp4";

    /**
     * 下载中的临时文件后缀：下载完成后才改名成正式文件，
     * 避免下载中途被当成"已下载"
     */
    private static final String DOWNLOADING_SUFFIX = "downloading";

    /**
     * 下载状态保留时长，超过后不再返回给前端
     */
    private static final long DOWNLOAD_STATE_KEEP_MILLIS = 30 * 60 * 1000L;

    private static final String STATE_RUNNING = "running";
    private static final String STATE_SUCCESS = "success";
    private static final String STATE_FAIL = "fail";

    @Autowired
    private BilibiliService bilibiliService;

    @Autowired
    private BilibiliVideoSqliteService bilibiliVideoSqliteService;

    /**
     * 下载状态，key: 视频记录id
     */
    private final Map<Long, DownloadState> downloadStates = new ConcurrentHashMap<>();

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
        IPage<BilibiliVideoSqlite> sourcePage = bilibiliVideoSqliteService.page(
                new Page<>(req.getCurrentPage(), req.getPageSize()), queryWrapper);

        Page<BilibiliVideoResp> targetPage = new Page<>(sourcePage.getCurrent(), sourcePage.getSize(), sourcePage.getTotal());
        targetPage.setRecords(sourcePage.getRecords().stream().map(this::toResp).toList());
        return targetPage;
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
        distinctIds.forEach(downloadStates::remove);
        bilibiliVideoSqliteService.removeByIds(distinctIds);
        return distinctIds.size();
    }

    /**
     * 下载视频到本地。
     * <p>
     * 下载地址由 {@link FileUtil#getBilibiliVideoFileName(String, Long, String)} 决定，
     * 会先调用 getPlayUrlInfo 获取下载链接（同时更新 play_url_raw），再异步下载
     * @return 下载状态
     */
    public DownloadState startDownload(Long id) {
        BilibiliVideoSqlite entity = bilibiliVideoSqliteService.getById(id);
        if (Objects.isNull(entity)) {
            throw new BusinessException("视频记录不存在");
        }
        if (isDownloaded(entity)) {
            throw new BusinessException("该视频已下载，无需重复下载");
        }
        DownloadState state = downloadStates.get(id);
        if (Objects.nonNull(state) && STATE_RUNNING.equals(state.getState())) {
            throw new BusinessException("该视频正在下载中");
        }
        DownloadState running = new DownloadState(id, STATE_RUNNING, null, System.currentTimeMillis());
        downloadStates.put(id, running);
        ThreadPoolUtil.getHandleCommandPool().execute(() -> doDownload(entity, running));
        return running;
    }

    /**
     * 下载任务，在独立线程中执行
     */
    private void doDownload(BilibiliVideoSqlite entity, DownloadState state) {
        File target = new File(FileUtil.getBilibiliVideoFileName(entity.getBvid(), entity.getCid(), VIDEO_SUFFIX));
        File downloading = new File(target.getAbsolutePath() + "." + DOWNLOADING_SUFFIX);
        try {
            BilibiliBaseResp<PlayUrlInfo> playUrlResp = bilibiliService.getPlayUrlInfo(entity.getBvid(), entity.getAvid(), entity.getCid());
            String url = Objects.isNull(playUrlResp) || Objects.isNull(playUrlResp.getData())
                    ? null : playUrlResp.getData().getDurlFirst();
            if (StringUtils.isBlank(url)) {
                throw new BusinessException("未获取到视频下载链接");
            }
            FileUtil.mkdirs(target.getParent());
            if (downloading.exists() && !downloading.delete()) {
                log.warn("删除残留的下载临时文件失败 {}", downloading.getAbsolutePath());
            }
            log.info("webui开始下载b站视频 {} -> {}", url, target.getAbsolutePath());
            long start = System.currentTimeMillis();
            bilibiliService.downloadVideo(url, downloading, -1);
            if (!downloading.renameTo(target)) {
                throw new BusinessException("视频文件重命名失败：" + target.getAbsolutePath());
            }
            state.success();
            log.info("webui下载b站视频完成 {} cost:{}", target.getAbsolutePath(), System.currentTimeMillis() - start);
        } catch (Exception e) {
            state.fail(e.getMessage());
            DbLog.error(BusinessModuleEnum.BILIBILI, "webui下载b站视频失败 bvid:{} cid:{}", entity.getBvid(), entity.getCid(), e);
            if (downloading.exists()) {
                downloading.delete();
            }
        }
    }

    /**
     * 下载状态，只返回最近一段时间内的记录
     */
    public Map<Long, DownloadState> downloadStates(List<Long> ids) {
        pruneDownloadStates();
        if (CollectionUtils.isEmpty(ids)) {
            return Collections.emptyMap();
        }
        Map<Long, DownloadState> result = new LinkedHashMap<>();
        for (Long id : ids) {
            DownloadState state = Objects.isNull(id) ? null : downloadStates.get(id);
            if (Objects.nonNull(state)) {
                result.put(id, state);
            }
        }
        return result;
    }

    private void pruneDownloadStates() {
        long now = System.currentTimeMillis();
        downloadStates.entrySet().removeIf(entry -> !STATE_RUNNING.equals(entry.getValue().getState())
                && now - entry.getValue().getUpdateTime() > DOWNLOAD_STATE_KEEP_MILLIS);
    }

    /**
     * 实体转卡片数据
     */
    public BilibiliVideoResp toResp(BilibiliVideoSqlite entity) {
        BilibiliVideoResp resp = new BilibiliVideoResp();
        BeanUtils.copyProperties(entity, resp);
        resp.setHasPlayUrl(StringUtils.isNotBlank(entity.getPlayUrlRaw()));
        resp.setVideoFileName(videoFileName(entity));
        resp.setDownloaded(isDownloaded(entity));
        DownloadState state = Objects.isNull(entity.getId()) ? null : downloadStates.get(entity.getId());
        if (Objects.nonNull(state)) {
            resp.setDownloading(STATE_RUNNING.equals(state.getState()));
            resp.setDownloadState(state.getState());
            resp.setDownloadMessage(state.getMessage());
        } else {
            resp.setDownloading(false);
        }
        return resp;
    }

    /**
     * 本地视频文件名，未下载时也能提前知道下载地址
     */
    public String videoFileName(BilibiliVideoSqlite entity) {
        return entity.getBvid() + "_" + entity.getCid() + "." + VIDEO_SUFFIX;
    }

    /**
     * 本地视频文件是否已存在
     */
    public boolean isDownloaded(BilibiliVideoSqlite entity) {
        if (Objects.isNull(entity) || StringUtils.isBlank(entity.getBvid()) || Objects.isNull(entity.getCid())) {
            return false;
        }
        return new File(FileUtil.getBilibiliVideoFileName(entity.getBvid(), entity.getCid(), VIDEO_SUFFIX)).exists();
    }

    /**
     * 视频下载状态
     */
    @Data
    public static class DownloadState {
        private Long id;
        private String state;
        private String message;
        private Long updateTime;

        public DownloadState() {
        }

        public DownloadState(Long id, String state, String message, Long updateTime) {
            this.id = id;
            this.state = state;
            this.message = message;
            this.updateTime = updateTime;
        }

        public void success() {
            this.state = STATE_SUCCESS;
            this.message = null;
            this.updateTime = System.currentTimeMillis();
        }

        public void fail(String message) {
            this.state = STATE_FAIL;
            this.message = message;
            this.updateTime = System.currentTimeMillis();
        }
    }
}
