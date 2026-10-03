package com.haruhi.botserver.features.bilibili.service;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.haruhi.botserver.features.bilibili.client.model.bilibili.VideoDetail;
import com.haruhi.botserver.features.bilibili.persistence.entity.BilibiliVideoSqlite;
import com.haruhi.botserver.features.bilibili.persistence.mapper.BilibiliVideoSqliteMapper;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
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
