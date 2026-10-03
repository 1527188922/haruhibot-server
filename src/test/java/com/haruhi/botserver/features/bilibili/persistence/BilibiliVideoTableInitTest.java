package com.haruhi.botserver.features.bilibili.persistence;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.haruhi.botserver.features.bilibili.persistence.entity.BilibiliVideoSqlite;
import com.haruhi.botserver.features.bilibili.persistence.mapper.BilibiliVideoSqliteMapper;
import com.haruhi.botserver.infrastructure.persistence.DataBaseConst;
import com.haruhi.botserver.infrastructure.persistence.persistence.mapper.SqliteDatabaseInitMapper;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * t_bilibili_video 建表语句（SqliteDatabaseInitMapper.xml）的真实测试。
 * <p>
 * 直接执行生产环境启动时用的那段 sql，验证：
 * <ul>
 *     <li>建表语句的列与实体列完全对应（少列会让 MyBatis-Plus 的查询直接报 no such column）</li>
 *     <li>bvid+cid 的唯一索引真的生效</li>
 * </ul>
 * 手写建表语句的测试无法发现这类漂移，所以这里必须用真实的 xml。
 */
class BilibiliVideoTableInitTest {

    private static final String MAPPER_XML = "mapper/infrastructure/persistence/SqliteDatabaseInitMapper.xml";

    private SqlSession session;

    @BeforeEach
    void openDatabase() throws Exception {
        MybatisConfiguration config = new MybatisConfiguration();
        config.setEnvironment(new Environment("test", new JdbcTransactionFactory(),
                new UnpooledDataSource("org.sqlite.JDBC", "jdbc:sqlite::memory:", null)));
        config.addMapper(BilibiliVideoSqliteMapper.class);
        MybatisPlusInterceptor pagination = new MybatisPlusInterceptor();
        pagination.addInnerInterceptor(new PaginationInnerInterceptor(DbType.SQLITE));
        config.addInterceptor(pagination);
        // 解析生产用的建表xml
        try (InputStream in = Resources.getResourceAsStream(MAPPER_XML)) {
            new XMLMapperBuilder(in, config, MAPPER_XML, config.getSqlFragments()).parse();
        }
        session = new MybatisSqlSessionFactoryBuilder().build(config).openSession(true);

        SqliteDatabaseInitMapper initMapper = session.getMapper(SqliteDatabaseInitMapper.class);
        initMapper.createBilibiliVideo(DataBaseConst.T_BILIBILI_VIDEO);
        initMapper.createIndexEnhance(DataBaseConst.T_BILIBILI_VIDEO,
                DataBaseConst.T_BILIBILI_VIDEO + "_bvid_cid_idx", "bvid,cid", true);
    }

    @AfterEach
    void closeDatabase() {
        if (session != null) {
            session.close();
        }
    }

    /**
     * 实体的每一列都要能在真实建表语句里查到
     */
    @Test
    void entityColumnsExistInCreateTableSql() {
        BilibiliVideoSqlite entity = new BilibiliVideoSqlite();
        entity.setBvid("BV1ZsaB6HE2U");
        entity.setCid(42369091370L);
        entity.setAvid(117365299747506L);
        entity.setTitle("标题");
        entity.setTag("财经,银行");
        entity.setDesc("简介");
        entity.setPic("http://i1.hdslb.com/bfs/archive/pic.jpg");
        entity.setDuration(441L);
        entity.setOwnerMid(55063151L);
        entity.setOwnerName("-黄同学run-");
        entity.setOwnerFace("https://i2.hdslb.com/bfs/face/face.jpg");
        entity.setVideoDetailRaw("{\"code\":0}");
        entity.setPlayUrlRaw("{\"play\":\"url\"}");
        entity.setPubdate(1790852544L);
        entity.setCtime(1790852545L);
        entity.setCreateTime("2026-10-03 16:00:00");
        entity.setUpdateTime("2026-10-03 16:00:00");
        session.getMapper(BilibiliVideoSqliteMapper.class).insert(entity);

        List<BilibiliVideoSqlite> rows = session.getMapper(BilibiliVideoSqliteMapper.class).selectList(null);
        assertEquals(1, rows.size());
        BilibiliVideoSqlite row = rows.getFirst();
        assertEquals("简介", row.getDesc());
        assertEquals("财经,银行", row.getTag());
        assertEquals("{\"play\":\"url\"}", row.getPlayUrlRaw());
        assertEquals(1790852544L, row.getPubdate());
    }

    @Test
    void bvidAndCidIndexIsUnique() {
        BilibiliVideoSqlite first = new BilibiliVideoSqlite();
        first.setBvid("BV1ZsaB6HE2U");
        first.setCid(42369091370L);
        session.getMapper(BilibiliVideoSqliteMapper.class).insert(first);

        BilibiliVideoSqlite duplicate = new BilibiliVideoSqlite();
        duplicate.setBvid("BV1ZsaB6HE2U");
        duplicate.setCid(42369091370L);

        assertThrows(Exception.class, () -> session.getMapper(BilibiliVideoSqliteMapper.class).insert(duplicate),
                "同一个bv号+cid不允许插入两次");
    }
}
