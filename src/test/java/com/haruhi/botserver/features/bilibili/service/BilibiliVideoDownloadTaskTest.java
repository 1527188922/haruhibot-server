package com.haruhi.botserver.features.bilibili.service;

import cn.hutool.core.io.StreamProgress;
import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.haruhi.botserver.features.bilibili.client.model.bilibili.BilibiliBaseResp;
import com.haruhi.botserver.features.bilibili.client.model.bilibili.PlayUrlInfo;
import com.haruhi.botserver.features.bilibili.model.BilibiliVideoDownloadSnapshot;
import com.haruhi.botserver.features.bilibili.model.BilibiliVideoDownloadTask;
import com.haruhi.botserver.features.bilibili.persistence.entity.BilibiliVideoSqlite;
import com.haruhi.botserver.features.bilibili.persistence.mapper.BilibiliVideoSqliteMapper;
import com.haruhi.botserver.shared.error.BusinessException;
import com.haruhi.botserver.shared.util.FileUtil;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * b站视频下载任务的真实测试。
 * <p>
 * 用可控制的假 {@link BilibiliService} 替代真实网络请求，覆盖：
 * 同一个 bvid+cid 只能有一个进行中的任务、已下载不允许重复下载、
 * 状态回写（成功/失败原因）、失败清理临时文件。
 */
class BilibiliVideoDownloadTaskTest {

    private static final String BVID = "BV1ZsaB6HE2U";
    private static final AtomicInteger CID_SEQ = new AtomicInteger();

    /**
     * 每个用例用不同的cid，避免异步下载写出的文件互相干扰
     */
    private long cid;
    private SqlSession session;
    private BilibiliVideoDownloadService downloadService;
    private FakeBilibiliService fakeBilibiliService;
    private File targetFile;

    @BeforeEach
    void setUp() throws Exception {
        MybatisConfiguration config = new MybatisConfiguration();
        config.setEnvironment(new Environment("test", new JdbcTransactionFactory(),
                new UnpooledDataSource("org.sqlite.JDBC", "jdbc:sqlite::memory:", null)));
        config.addMapper(BilibiliVideoSqliteMapper.class);
        MybatisPlusInterceptor pagination = new MybatisPlusInterceptor();
        pagination.addInnerInterceptor(new PaginationInnerInterceptor(DbType.SQLITE));
        config.addInterceptor(pagination);
        session = new MybatisSqlSessionFactoryBuilder().build(config).openSession(true);
        try (var statement = session.getConnection().createStatement()) {
            statement.execute("CREATE TABLE t_bilibili_video ("
                    + "`id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, `bvid` TEXT NOT NULL, `avid` INTEGER,"
                    + "`cid` INTEGER NOT NULL, `title` TEXT, `tag` TEXT, `desc` TEXT, `pic` TEXT, `duration` INTEGER,"
                    + "`owner_mid` INTEGER, `owner_name` TEXT, `owner_face` TEXT, `video_detail_raw` TEXT,"
                    + "`play_url_raw` TEXT, `pubdate` INTEGER, `ctime` INTEGER,"
                    + "`create_time` DATETIME, `update_time` DATETIME)");
            statement.execute("CREATE UNIQUE INDEX t_bilibili_video_bvid_cid_idx ON t_bilibili_video (bvid, cid)");
        }

        cid = 42369091370L + CID_SEQ.incrementAndGet();
        fakeBilibiliService = new FakeBilibiliService();
        downloadService = new BilibiliVideoDownloadService();
        inject(downloadService, "bilibiliService", fakeBilibiliService);
        targetFile = new File(FileUtil.getBilibiliVideoFileName(BVID, cid, BilibiliVideoDownloadService.VIDEO_SUFFIX));
        deleteQuietly(targetFile);
        deleteQuietly(downloadingFile());
    }

    @AfterEach
    void tearDown() {
        deleteQuietly(targetFile);
        deleteQuietly(downloadingFile());
        if (session != null) {
            session.close();
        }
    }

    /**
     * 同一个 bvid+cid 同时只能有一个正在下载的任务
     */
    @Test
    void onlyOneRunningTaskPerBvidAndCid() throws Exception {
        fakeBilibiliService.gate = new CountDownLatch(1);
        BilibiliVideoDownloadTask first = downloadService.start(entity());

        assertEquals(BilibiliVideoDownloadTask.STATUS_RUNNING, first.getStatus());
        assertNotNull(downloadService.runningTask(BVID, cid));

        BusinessException e = assertThrows(BusinessException.class, () -> downloadService.start(entity()));
        assertTrue(e.getMessage().contains("正在下载中"), e.getMessage());

        fakeBilibiliService.gate.countDown();
        BilibiliVideoDownloadTask finished = awaitFinished();
        assertEquals(BilibiliVideoDownloadTask.STATUS_SUCCESS, finished.getStatus());
        assertNull(finished.getMessage(), "下载成功不应有失败原因");
        assertNotNull(finished.getCostMillis());
        assertTrue(targetFile.exists(), "下载完成后本地应有正式文件");
        assertNull(downloadService.runningTask(BVID, cid), "任务结束后不应再有进行中的任务");
    }

    /**
     * 本地已存在视频文件时不允许重复下载
     */
    @Test
    void rejectWhenFileAlreadyDownloaded() throws Exception {
        FileUtil.mkdirs(targetFile.getParent());
        Files.write(targetFile.toPath(), new byte[]{1, 2, 3});

        BusinessException e = assertThrows(BusinessException.class, () -> downloadService.start(entity()));
        assertTrue(e.getMessage().contains("已下载"), e.getMessage());
    }

    /**
     * 下载失败要记录原因并清掉临时文件
     */
    @Test
    void failedTaskKeepsMessageAndRemovesTempFile() throws Exception {
        fakeBilibiliService.failMessage = "连接超时";
        downloadService.start(entity());

        BilibiliVideoDownloadTask finished = awaitFinished();
        assertEquals(BilibiliVideoDownloadTask.STATUS_FAIL, finished.getStatus());
        assertEquals("连接超时", finished.getMessage());
        assertFalse(targetFile.exists());
        assertFalse(downloadingFile().exists(), "失败后临时文件要清掉");
    }

    /**
     * 快照统计：进行中/成功/失败
     */
    @Test
    void snapshotCounters() throws Exception {
        downloadService.start(entity());
        awaitFinished();

        BilibiliVideoDownloadSnapshot snapshot = downloadService.snapshot();
        assertEquals(0, snapshot.getCounters().getRunning());
        assertEquals(1, snapshot.getCounters().getSuccess());
        assertEquals(0, snapshot.getCounters().getFail());
        assertEquals(1, snapshot.getFinishedList().size());
    }

    private File downloadingFile() {
        return new File(targetFile.getAbsolutePath() + ".downloading");
    }

    private BilibiliVideoDownloadTask awaitFinished() throws InterruptedException {
        long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(10);
        while (System.currentTimeMillis() < deadline) {
            BilibiliVideoDownloadTask task = downloadService.lastTask(BVID, cid);
            if (task != null && !task.isRunning()) {
                return task;
            }
            Thread.sleep(20);
        }
        throw new AssertionError("下载任务未在10秒内结束");
    }

    private BilibiliVideoSqlite entity() {
        BilibiliVideoSqlite entity = new BilibiliVideoSqlite();
        entity.setId(1L);
        entity.setBvid(BVID);
        entity.setCid(cid);
        entity.setAvid(117365299747506L);
        entity.setTitle("标题A");
        return entity;
    }

    private static void deleteQuietly(File file) {
        if (file != null && file.exists() && !file.delete()) {
            file.deleteOnExit();
        }
    }

    private static void inject(Object target, String fieldName, Object value) throws Exception {
        Class<?> type = target.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(target, value);
                return;
            } catch (NoSuchFieldException e) {
                type = type.getSuperclass();
            }
        }
        throw new NoSuchFieldException(fieldName);
    }

    /**
     * 假的b站客户端：不访问网络，用一个闸门控制下载何时结束，便于观察"进行中"的状态
     */
    private static class FakeBilibiliService extends BilibiliService {

        private CountDownLatch gate = new CountDownLatch(0);
        private String failMessage;

        @Override
        public BilibiliBaseResp<PlayUrlInfo> getPlayUrlInfo(String bvid, Long avid, Long cid) {
            BilibiliBaseResp<PlayUrlInfo> resp = new BilibiliBaseResp<>();
            resp.setCode(BilibiliBaseResp.SUCCESS_CODE);
            PlayUrlInfo info = new PlayUrlInfo();
            PlayUrlInfo.Durl durl = new PlayUrlInfo.Durl();
            durl.setUrl("http://127.0.0.1/fake.mp4");
            info.setDurl(List.of(durl));
            resp.setData(info);
            return resp;
        }

        @Override
        public void downloadVideo(String url, File file, int timeout, StreamProgress progress) {
            try {
                gate.await(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            if (failMessage != null) {
                throw new BusinessException(failMessage);
            }
            try {
                FileUtil.mkdirs(file.getParent());
                Files.write(file.toPath(), new byte[2048]);
            } catch (Exception e) {
                throw new BusinessException("写测试文件失败：" + e.getMessage());
            }
            if (progress != null) {
                // 下载不再上报进度，这里只是保证接口仍可被调用
                progress.start();
                progress.progress(2048, 2048);
                progress.finish();
            }
        }
    }
}
