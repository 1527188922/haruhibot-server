package com.haruhi.botserver.features.bilibili.service;

import com.haruhi.botserver.features.bilibili.client.model.bilibili.BilibiliBaseResp;
import com.haruhi.botserver.features.bilibili.client.model.bilibili.PlayUrlInfo;
import com.haruhi.botserver.features.bilibili.model.BilibiliVideoDownloadSnapshot;
import com.haruhi.botserver.features.bilibili.model.BilibiliVideoDownloadTask;
import com.haruhi.botserver.features.bilibili.persistence.entity.BilibiliVideoSqlite;
import com.haruhi.botserver.infrastructure.concurrent.ThreadPoolUtil;
import com.haruhi.botserver.infrastructure.logging.DbLog;
import com.haruhi.botserver.shared.constant.BusinessModuleEnum;
import com.haruhi.botserver.shared.error.BusinessException;
import com.haruhi.botserver.shared.util.FileUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * b站视频下载任务
 * <p>
 * 与JM任务队列一样只在内存中：任务状态不持久化，重启即清空；
 * "是否已下载"始终以本地磁盘上的文件为准。
 * <p>
 * 同一个 bvid+cid 只允许有一个正在下载的任务（任务id就是 bvid_cid）。
 */
@Service
@Slf4j
public class BilibiliVideoDownloadService {

    /**
     * 本地视频文件后缀，与 {@link com.haruhi.botserver.features.bilibili.handler.BilibiliVideoParseHandler} 保持一致
     */
    public static final String VIDEO_SUFFIX = "mp4";

    /**
     * 下载中的临时文件后缀：下载完成后才改名成正式文件，
     * 避免下载中途被当成"已下载"
     */
    private static final String DOWNLOADING_SUFFIX = "downloading";

    /**
     * 已完成任务最多保留的条数（页面上的"最近完成"）
     */
    private static final int MAX_FINISHED_TASKS = 20;

    private static final String STATUS_NAME_RUNNING = "下载中";
    private static final String STATUS_NAME_SUCCESS = "已完成";
    private static final String STATUS_NAME_FAIL = "失败";

    @Autowired
    private BilibiliService bilibiliService;

    /**
     * 任务表，key: bvid_cid
     */
    private final Map<String, BilibiliVideoDownloadTask> tasks = new ConcurrentHashMap<>();

    /**
     * 任务变化通知（进度/状态变化时置脏），由推送服务注册
     */
    private volatile Runnable changeNotifier;

    public void setChangeNotifier(Runnable changeNotifier) {
        this.changeNotifier = changeNotifier;
    }

    /**
     * 视频本地文件，即 {@link FileUtil#getBilibiliVideoFileName(String, Long, String)}
     */
    public File videoFile(BilibiliVideoSqlite entity) {
        return new File(FileUtil.getBilibiliVideoFileName(entity.getBvid(), entity.getCid(), VIDEO_SUFFIX));
    }

    public boolean isDownloaded(BilibiliVideoSqlite entity) {
        if (Objects.isNull(entity) || StringUtils.isBlank(entity.getBvid()) || Objects.isNull(entity.getCid())) {
            return false;
        }
        return videoFile(entity).exists();
    }

    /**
     * 某条视频当前正在下载的任务，没有则返回null
     */
    public BilibiliVideoDownloadTask runningTask(String bvid, Long cid) {
        if (StringUtils.isBlank(bvid) || Objects.isNull(cid)) {
            return null;
        }
        BilibiliVideoDownloadTask task = tasks.get(BilibiliVideoDownloadTask.taskId(bvid, cid));
        return task != null && task.isRunning() ? copy(task) : null;
    }

    /**
     * 某条视频最近一次下载任务（含已完成），没有则返回null
     */
    public BilibiliVideoDownloadTask lastTask(String bvid, Long cid) {
        if (StringUtils.isBlank(bvid) || Objects.isNull(cid)) {
            return null;
        }
        BilibiliVideoDownloadTask task = tasks.get(BilibiliVideoDownloadTask.taskId(bvid, cid));
        return Objects.isNull(task) ? null : copy(task);
    }

    /**
     * 开始下载。
     * <p>
     * 同一个 bvid+cid 已存在进行中的任务、或本地文件已存在时直接拒绝
     */
    public BilibiliVideoDownloadTask start(BilibiliVideoSqlite entity) {
        if (Objects.isNull(entity) || StringUtils.isBlank(entity.getBvid()) || Objects.isNull(entity.getCid())) {
            throw new BusinessException("视频信息不完整，无法下载");
        }
        String taskId = BilibiliVideoDownloadTask.taskId(entity.getBvid(), entity.getCid());
        BilibiliVideoDownloadTask exist = tasks.get(taskId);
        if (Objects.nonNull(exist) && exist.isRunning()) {
            throw new BusinessException("该视频正在下载中，请勿重复提交");
        }
        if (isDownloaded(entity)) {
            throw new BusinessException("该视频已下载，无需重复下载");
        }

        BilibiliVideoDownloadTask task = new BilibiliVideoDownloadTask();
        task.setTaskId(taskId);
        task.setVideoId(entity.getId());
        task.setBvid(entity.getBvid());
        task.setCid(entity.getCid());
        task.setAvid(entity.getAvid());
        task.setTitle(entity.getTitle());
        task.setCoverUrl(entity.getPic());
        task.setFileName(videoFile(entity).getName());
        task.setStatus(BilibiliVideoDownloadTask.STATUS_RUNNING);
        task.setStatusName(STATUS_NAME_RUNNING);
        task.setStartTime(System.currentTimeMillis());
        tasks.put(taskId, task);
        trimFinished();
        notifyChange();

        ThreadPoolUtil.getHandleCommandPool().execute(() -> doDownload(entity, task));
        return copy(task);
    }

    private void doDownload(BilibiliVideoSqlite entity, BilibiliVideoDownloadTask task) {
        File target = videoFile(entity);
        File downloading = new File(target.getAbsolutePath() + "." + DOWNLOADING_SUFFIX);
        long start = System.currentTimeMillis();
        try {
            // 调用getPlayUrlInfo获取下载链接，同时会把响应raw更新到 play_url_raw
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
            log.info("webui开始下载b站视频 bvid:{} cid:{} -> {}", entity.getBvid(), entity.getCid(), target.getAbsolutePath());
            // 不关心下载进度：不统计总字节数/已下载字节数，只按状态推送
            bilibiliService.downloadVideo(url, downloading, -1);
            if (!downloading.renameTo(target)) {
                throw new BusinessException("视频文件重命名失败：" + target.getAbsolutePath());
            }
            synchronized (task) {
                task.setStatus(BilibiliVideoDownloadTask.STATUS_SUCCESS);
                task.setStatusName(STATUS_NAME_SUCCESS);
                task.setEndTime(System.currentTimeMillis());
                task.setCostMillis(task.getEndTime() - start);
                task.setMessage(null);
            }
            notifyChange();
            log.info("webui下载b站视频完成 bvid:{} cid:{} cost:{}", entity.getBvid(), entity.getCid(), System.currentTimeMillis() - start);
        } catch (Exception e) {
            synchronized (task) {
                task.setStatus(BilibiliVideoDownloadTask.STATUS_FAIL);
                task.setStatusName(STATUS_NAME_FAIL);
                task.setEndTime(System.currentTimeMillis());
                task.setCostMillis(task.getEndTime() - start);
                task.setMessage(StringUtils.defaultIfBlank(e.getMessage(), e.getClass().getSimpleName()));
            }
            notifyChange();
            DbLog.error(BusinessModuleEnum.BILIBILI, "webui下载b站视频失败 bvid:{} cid:{}", entity.getBvid(), entity.getCid(), e);
            if (downloading.exists() && !downloading.delete()) {
                log.warn("删除下载失败的临时文件失败 {}", downloading.getAbsolutePath());
            }
        }
    }

    /**
     * 任务快照，供WebSocket推送与页面轮询使用
     */
    public BilibiliVideoDownloadSnapshot snapshot() {
        List<BilibiliVideoDownloadTask> all = tasks.values().stream().map(this::copy).toList();
        List<BilibiliVideoDownloadTask> running = all.stream()
                .filter(BilibiliVideoDownloadTask::isRunning)
                .sorted(Comparator.comparing(BilibiliVideoDownloadTask::getStartTime,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        List<BilibiliVideoDownloadTask> finished = all.stream()
                .filter(task -> !task.isRunning())
                .sorted(Comparator.comparing(BilibiliVideoDownloadTask::getEndTime,
                        Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .toList();

        BilibiliVideoDownloadSnapshot.Counters counters = new BilibiliVideoDownloadSnapshot.Counters();
        counters.setRunning(running.size());
        counters.setSuccess((int) finished.stream().filter(task -> BilibiliVideoDownloadTask.STATUS_SUCCESS.equals(task.getStatus())).count());
        counters.setFail((int) finished.stream().filter(task -> BilibiliVideoDownloadTask.STATUS_FAIL.equals(task.getStatus())).count());

        BilibiliVideoDownloadSnapshot snapshot = new BilibiliVideoDownloadSnapshot();
        snapshot.setCounters(counters);
        snapshot.setRunningList(new ArrayList<>(running));
        snapshot.setFinishedList(new ArrayList<>(finished));
        return snapshot;
    }

    /**
     * 清理掉最早的已完成任务，只保留最近 {@link #MAX_FINISHED_TASKS} 条
     */
    private void trimFinished() {
        List<BilibiliVideoDownloadTask> finished = tasks.values().stream()
                .filter(task -> !task.isRunning())
                .sorted(Comparator.comparing(BilibiliVideoDownloadTask::getEndTime,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        if (CollectionUtils.size(finished) <= MAX_FINISHED_TASKS) {
            return;
        }
        finished.subList(0, finished.size() - MAX_FINISHED_TASKS).forEach(task -> tasks.remove(task.getTaskId()));
    }

    private void notifyChange() {
        Runnable notifier = this.changeNotifier;
        if (Objects.nonNull(notifier)) {
            notifier.run();
        }
    }

    /**
     * 拷贝一份任务，避免推送线程读到正在被下载线程修改的对象
     */
    private BilibiliVideoDownloadTask copy(BilibiliVideoDownloadTask task) {
        BilibiliVideoDownloadTask copy = new BilibiliVideoDownloadTask();
        synchronized (task) {
            copy.setTaskId(task.getTaskId());
            copy.setVideoId(task.getVideoId());
            copy.setBvid(task.getBvid());
            copy.setCid(task.getCid());
            copy.setAvid(task.getAvid());
            copy.setTitle(task.getTitle());
            copy.setCoverUrl(task.getCoverUrl());
            copy.setFileName(task.getFileName());
            copy.setStatus(task.getStatus());
            copy.setStatusName(task.getStatusName());
            copy.setStartTime(task.getStartTime());
            copy.setEndTime(task.getEndTime());
            copy.setCostMillis(task.getCostMillis());
            copy.setMessage(task.getMessage());
        }
        return copy;
    }
}
