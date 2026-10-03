package com.haruhi.botserver.features.bilibili.service;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.haruhi.botserver.features.bilibili.client.model.bilibili.VideoDetail;
import com.haruhi.botserver.features.bilibili.model.BilibiliVideoAuthorResp;
import com.haruhi.botserver.features.bilibili.model.BilibiliVideoDeleteReq;
import com.haruhi.botserver.features.bilibili.model.BilibiliVideoQueryReq;
import com.haruhi.botserver.features.bilibili.model.BilibiliVideoTagResp;
import com.haruhi.botserver.features.bilibili.persistence.entity.BilibiliVideoSqlite;
import com.haruhi.botserver.features.bilibili.persistence.mapper.BilibiliVideoSqliteMapper;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * b站视频入库的真实数据库测试（内存SQLite + 真实Mapper）。
 * <p>
 * 覆盖点：详情接口按 bv+cid 插入/更新、更新时不覆盖 play_url_raw、
 * 下载链接接口写入 play_url_raw、标签逗号拼接、desc 关键字列可读写。
 */
class BilibiliVideoSqliteServiceTest {

    private static final String BVID = "BV1ZsaB6HE2U";
    private static final long CID = 42369091370L;

    private SqlSession session;
    private BilibiliVideoSqliteServiceImpl service;

    @BeforeEach
    void openDatabase() throws Exception {
        MybatisConfiguration config = new MybatisConfiguration();
        config.setEnvironment(new Environment("test", new JdbcTransactionFactory(),
                new UnpooledDataSource("org.sqlite.JDBC", "jdbc:sqlite::memory:", null)));
        config.addMapper(BilibiliVideoSqliteMapper.class);
        MybatisPlusInterceptor pagination = new MybatisPlusInterceptor();
        pagination.addInnerInterceptor(new PaginationInnerInterceptor(DbType.SQLITE));
        config.addInterceptor(pagination);
        session = new MybatisSqlSessionFactoryBuilder().build(config).openSession(true);

        try (var statement = session.getConnection().createStatement()) {
            // 必须与 SqliteDatabaseInitMapper.xml 的 createBilibiliVideo 完全一致
            statement.execute("CREATE TABLE t_bilibili_video ("
                    + "`id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, `bvid` TEXT NOT NULL, `avid` INTEGER,"
                    + "`cid` INTEGER NOT NULL, `title` TEXT, `tag` TEXT, `desc` TEXT, `pic` TEXT, `duration` INTEGER,"
                    + "`owner_mid` INTEGER, `owner_name` TEXT, `owner_face` TEXT, `video_detail_raw` TEXT,"
                    + "`play_url_raw` TEXT, `pubdate` INTEGER, `ctime` INTEGER,"
                    + "`create_time` DATETIME, `update_time` DATETIME)");
            statement.execute("CREATE UNIQUE INDEX t_bilibili_video_bvid_cid_idx ON t_bilibili_video (bvid, cid)");
        }

        service = new BilibiliVideoSqliteServiceImpl();
        Field field = ServiceImpl.class.getDeclaredField("baseMapper");
        field.setAccessible(true);
        field.set(service, session.getMapper(BilibiliVideoSqliteMapper.class));
    }

    @AfterEach
    void closeDatabase() {
        if (session != null) {
            session.close();
        }
    }

    @Test
    void insertWhenVideoNotExists() {
        BilibiliVideoSqlite saved = service.saveOrUpdateByVideoDetail(detail("标题A", 441L), "{\"code\":0}");

        assertNotNull(saved);
        List<BilibiliVideoSqlite> all = service.list();
        assertEquals(1, all.size());
        BilibiliVideoSqlite row = all.getFirst();
        assertEquals(BVID, row.getBvid());
        assertEquals(CID, row.getCid());
        assertEquals(117365299747506L, row.getAvid());
        assertEquals("标题A", row.getTitle());
        assertEquals("万物研究所,财经,银行", row.getTag(), "多个标签用逗号拼接");
        assertEquals("简介内容", row.getDesc(), "desc 是sql关键字，需要能正常读写");
        assertEquals("http://i1.hdslb.com/bfs/archive/pic.jpg", row.getPic());
        assertEquals(441L, row.getDuration());
        assertEquals(55063151L, row.getOwnerMid());
        assertEquals("-黄同学run-", row.getOwnerName());
        assertEquals("https://i2.hdslb.com/bfs/face/face.jpg", row.getOwnerFace());
        assertEquals("{\"code\":0}", row.getVideoDetailRaw());
        assertNull(row.getPlayUrlRaw(), "详情接口不会写入play_url_raw");
        assertEquals(1790852544L, row.getPubdate());
        assertEquals(1790852545L, row.getCtime());
        assertNotNull(row.getCreateTime());
        assertNotNull(row.getUpdateTime());
    }

    /**
     * 每次调用详情接口都要按 bv+cid 更新，而不是再插一条
     */
    @Test
    void updateWhenVideoExists() {
        service.saveOrUpdateByVideoDetail(detail("标题A", 441L), "{\"code\":0,\"v\":1}");
        service.saveOrUpdateByVideoDetail(detail("标题B", 500L), "{\"code\":0,\"v\":2}");

        List<BilibiliVideoSqlite> all = service.list();
        assertEquals(1, all.size(), "bv+cid 相同只应有一条记录");
        BilibiliVideoSqlite row = all.getFirst();
        assertEquals("标题B", row.getTitle());
        assertEquals(500L, row.getDuration());
        assertEquals("{\"code\":0,\"v\":2}", row.getVideoDetailRaw());
    }

    /**
     * 详情接口的响应里没有播放地址，更新时不能把已经写入的 play_url_raw 冲掉
     */
    @Test
    void updateVideoDetailKeepsPlayUrlRaw() {
        service.saveOrUpdateByVideoDetail(detail("标题A", 441L), "{\"code\":0}");
        assertTrue(service.updatePlayUrlRaw(BVID, CID, "{\"play\":\"url\"}"));

        service.saveOrUpdateByVideoDetail(detail("标题B", 441L), "{\"code\":0,\"v\":2}");

        BilibiliVideoSqlite row = service.getByBvidAndCid(BVID, CID);
        assertEquals("{\"play\":\"url\"}", row.getPlayUrlRaw(), "详情接口的更新不能覆盖play_url_raw");
        assertEquals("标题B", row.getTitle());
    }

    /**
     * 同一个视频多个分p(cid不同)是两条记录
     */
    @Test
    void sameBvidDifferentCidIsAnotherRecord() {
        service.saveOrUpdateByVideoDetail(detail("标题A", 441L), "{\"code\":0}");
        VideoDetail other = detail("标题A-分p2", 100L);
        other.getView().setCid(999L);
        other.getView().getPages().getFirst().setCid(999L);
        service.saveOrUpdateByVideoDetail(other, "{\"code\":0}");

        assertEquals(2, service.list().size());
        assertEquals(CID, service.getByBvidAndCid(BVID, CID).getCid());
        assertEquals(999L, service.getByBvidAndCid(BVID, 999L).getCid());
    }

    @Test
    void updatePlayUrlRawWithoutRecordReturnsFalse() {
        assertFalse(service.updatePlayUrlRaw(BVID, CID, "{\"play\":\"url\"}"));
        assertNull(service.getByBvidAndCid(BVID, CID));
    }

    @Test
    void searchUsesCidFromPagesFirst() {
        VideoDetail detail = detail("标题A", 441L);
        detail.getView().setCid(null);
        BilibiliVideoSqlite saved = service.saveOrUpdateByVideoDetail(detail, "{\"code\":0}");
        assertEquals(CID, saved.getCid());
    }

    @Test
    void ignoreDetailWithoutView() {
        assertNull(service.saveOrUpdateByVideoDetail(new VideoDetail(), "{\"code\":-404}"));
        assertTrue(service.list().isEmpty());
    }

    /**
     * 左侧标签分组列表：逗号分隔的标签要拆开分别计数，按数量倒序
     */
    @Test
    void listTagsCountsEachTag() {
        saveRow("BV1", 1L, 100L, "作者A", "万物研究所,财经,银行");
        saveRow("BV2", 2L, 100L, "作者A", "财经,科普");
        saveRow("BV3", 3L, 200L, "作者B", null);

        List<BilibiliVideoTagResp> tags = service.listTags();
        assertEquals(List.of("财经", "万物研究所", "科普", "银行"),
                tags.stream().map(BilibiliVideoTagResp::getTag).toList());
        assertEquals(2L, tags.getFirst().getVideoCount());
        assertEquals(1L, tags.get(1).getVideoCount());
    }

    /**
     * 左侧作者分组列表：同一个mid的多条视频合并计数
     */
    @Test
    void listAuthorsCountsVideos() {
        saveRow("BV1", 1L, 100L, "作者A", null);
        saveRow("BV2", 2L, 100L, "作者A", null);
        saveRow("BV3", 3L, 200L, "作者B", null);

        List<BilibiliVideoAuthorResp> authors = service.listAuthors();
        assertEquals(2, authors.size());
        assertEquals(100L, authors.getFirst().getOwnerMid());
        assertEquals("作者A", authors.getFirst().getOwnerName());
        assertEquals(2L, authors.getFirst().getVideoCount());
        assertEquals(1L, authors.get(1).getVideoCount());
    }

    /**
     * 左侧标签点击后的精确过滤："财经"不能命中"财经商业"
     */
    @Test
    void searchByTagExactMatchesWholeTag() throws Exception {
        saveRow("BV1", 1L, 100L, "作者A", "财经,银行");
        saveRow("BV2", 2L, 100L, "作者A", "财经商业");

        BilibiliVideoService videoService = new BilibiliVideoService();
        inject(videoService, "bilibiliVideoSqliteService", service);
        inject(videoService, "bilibiliVideoDownloadService", new BilibiliVideoDownloadService());

        BilibiliVideoQueryReq req = new BilibiliVideoQueryReq();
        req.setTagExact("财经");
        assertEquals(1L, videoService.search(req).getTotal());
        assertEquals("BV1", videoService.search(req).getRecords().getFirst().getBvid());

        req.setTagExact("财经商业");
        assertEquals(1L, videoService.search(req).getTotal());
        assertEquals("BV2", videoService.search(req).getRecords().getFirst().getBvid());

        req.setTagExact("财");
        assertEquals(0L, videoService.search(req).getTotal());
    }

    /**
     * 左侧作者分组点击后按 ownerMid 过滤
     */
    @Test
    void searchByOwnerMid() throws Exception {
        saveRow("BV1", 1L, 100L, "作者A", null);
        saveRow("BV2", 2L, 200L, "作者B", null);

        BilibiliVideoService videoService = new BilibiliVideoService();
        inject(videoService, "bilibiliVideoSqliteService", service);
        inject(videoService, "bilibiliVideoDownloadService", new BilibiliVideoDownloadService());

        BilibiliVideoQueryReq req = new BilibiliVideoQueryReq();
        req.setOwnerMid(200L);
        assertEquals(1L, videoService.search(req).getTotal());
        assertEquals("BV2", videoService.search(req).getRecords().getFirst().getBvid());
    }

    /**
     * 批量删除：删数据库记录 / 删视频文件是两个独立勾选项，勾了哪个删哪个
     */
    @Test
    void deleteBatchByOptions() throws Exception {
        // 用不会和真实视频重名的 bvid/cid，避免动到开发机上的文件
        saveRow("BVDELTEST1", 999001L, 100L, "作者A", null);
        saveRow("BVDELTEST2", 999002L, 100L, "作者A", null);

        BilibiliVideoService videoService = new BilibiliVideoService();
        inject(videoService, "bilibiliVideoSqliteService", service);
        BilibiliVideoDownloadService downloadService = new BilibiliVideoDownloadService();
        inject(videoService, "bilibiliVideoDownloadService", downloadService);

        BilibiliVideoSqlite first = service.getByBvidAndCid("BVDELTEST1", 999001L);
        BilibiliVideoSqlite second = service.getByBvidAndCid("BVDELTEST2", 999002L);
        File firstFile = downloadService.videoFile(first);
        File secondFile = downloadService.videoFile(second);
        FileUtil.mkdirs(firstFile.getParent());
        Files.write(firstFile.toPath(), new byte[]{1, 2, 3});
        Files.write(secondFile.toPath(), new byte[]{1, 2, 3});
        try {
            // 只删文件：文件没了，记录还在
            String onlyFile = videoService.deleteBatch(deleteReq(List.of(first.getId()), false, true));
            assertTrue(onlyFile.contains("1个视频文件"), onlyFile);
            assertFalse(firstFile.exists(), "勾了删除视频文件就应该把文件删掉");
            assertNotNull(service.getById(first.getId()), "没勾删除数据库记录时记录要保留");

            // 只删记录：记录没了，文件还在
            String onlyData = videoService.deleteBatch(deleteReq(List.of(second.getId()), true, false));
            assertTrue(onlyData.contains("1条视频记录"), onlyData);
            assertNull(service.getById(second.getId()), "勾了删除数据库记录就应该删掉记录");
            assertTrue(secondFile.exists(), "没勾删除视频文件时文件要保留");
        } finally {
            Files.deleteIfExists(firstFile.toPath());
            Files.deleteIfExists(secondFile.toPath());
        }

        // 两个都勾：记录和文件一起删
        saveRow("BVDELTEST3", 999003L, 100L, "作者A", null);
        BilibiliVideoSqlite third = service.getByBvidAndCid("BVDELTEST3", 999003L);
        File thirdFile = downloadService.videoFile(third);
        Files.write(thirdFile.toPath(), new byte[]{1, 2, 3});
        String both = videoService.deleteBatch(deleteReq(List.of(third.getId()), true, true));
        assertTrue(both.contains("1条视频记录") && both.contains("1个视频文件"), both);
        assertNull(service.getById(third.getId()));
        assertFalse(thirdFile.exists());
    }

    private BilibiliVideoDeleteReq deleteReq(List<Long> ids, boolean deleteData, boolean deleteFile) {
        BilibiliVideoDeleteReq req = new BilibiliVideoDeleteReq();
        req.setIds(ids);
        req.setDeleteData(deleteData);
        req.setDeleteFile(deleteFile);
        return req;
    }

    /**
     * 直接落库一条视频（左侧分组统计只需要这几个字段）
     */
    private void saveRow(String bvid, long cid, Long ownerMid, String ownerName, String tags) {
        BilibiliVideoSqlite entity = new BilibiliVideoSqlite();
        entity.setBvid(bvid);
        entity.setCid(cid);
        entity.setOwnerMid(ownerMid);
        entity.setOwnerName(ownerName);
        entity.setOwnerFace("https://i2.hdslb.com/bfs/face/" + ownerMid + ".jpg");
        entity.setTitle("标题-" + bvid);
        entity.setTag(tags);
        entity.setPubdate(1790852544L + cid);
        service.save(entity);
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
     * 构造详情接口响应：View + 3个标签，cid 同时存在于 view 与 pages[0]
     */
    private VideoDetail detail(String title, Long duration) {
        VideoDetail detail = new VideoDetail();
        VideoDetail.View view = new VideoDetail.View();
        view.setBvid(BVID);
        view.setAid(117365299747506L);
        view.setCid(CID);
        view.setTitle(title);
        view.setDesc("简介内容");
        view.setPic("http://i1.hdslb.com/bfs/archive/pic.jpg");
        view.setDuration(duration);
        view.setPubdate(1790852544L);
        view.setCtime(1790852545L);

        VideoDetail.View.Owner owner = new VideoDetail.View.Owner();
        owner.setMid(55063151L);
        owner.setName("-黄同学run-");
        owner.setFace("https://i2.hdslb.com/bfs/face/face.jpg");
        view.setOwner(owner);

        VideoDetail.View.Page page = new VideoDetail.View.Page();
        page.setCid(CID);
        page.setPage(1);
        view.setPages(List.of(page));
        detail.setView(view);

        detail.setTags(List.of("万物研究所", "财经", "银行").stream().map(name -> {
            VideoDetail.Tag tag = new VideoDetail.Tag();
            tag.setTagName(name);
            return tag;
        }).toList());
        return detail;
    }
}
