package com.haruhi.botserver.features.jmcomic.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder;
import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.haruhi.botserver.features.jmcomic.model.JmAlbumCollectReq;
import com.haruhi.botserver.features.jmcomic.model.JmFavoriteAlbumRemoveReq;
import com.haruhi.botserver.features.jmcomic.model.JmFavoriteAlbumReq;
import com.haruhi.botserver.features.jmcomic.model.JmFavoriteResp;
import com.haruhi.botserver.features.jmcomic.persistence.entity.JmAlbumSqlite;
import com.haruhi.botserver.features.jmcomic.persistence.mapper.JmAlbumSqliteMapper;
import com.haruhi.botserver.features.jmcomic.persistence.mapper.JmChapterImageSqliteMapper;
import com.haruhi.botserver.features.jmcomic.persistence.mapper.JmFavoriteAlbumSqliteMapper;
import com.haruhi.botserver.features.jmcomic.persistence.mapper.JmFavoriteSqliteMapper;
import com.haruhi.botserver.shared.error.BusinessException;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JM收藏夹逻辑的真实数据库测试。
 * <p>
 * 用内存 SQLite + 真实 Mapper，而不是 Mockito：这个项目在 JDK 24 下 Mockito 无法加载
 * MockMaker（既有的 JmcomicServiceTest 同样受影响），所以走 KvStoreServiceTest 的
 * "UnpooledDataSource + MybatisSqlSessionFactoryBuilder" 范式。
 * <p>
 * 覆盖点：默认收藏夹、一对多/多对多、collected 标记同步、删除收藏夹的回落、默认收藏夹保护。
 */
class JmcomicFavoriteTest {

    private static final String DEFAULT_NAME = JmcomicSqliteServiceImpl.DEFAULT_FAVORITE_NAME;

    private SqlSession session;
    private JmcomicSqliteServiceImpl service;

    @BeforeEach
    void openDatabase() throws Exception {
        MybatisConfiguration config = new MybatisConfiguration();
        config.setEnvironment(new Environment("test", new JdbcTransactionFactory(),
                new UnpooledDataSource("org.sqlite.JDBC", "jdbc:sqlite::memory:", null)));
        config.addMapper(JmAlbumSqliteMapper.class);
        config.addMapper(JmChapterImageSqliteMapper.class);
        config.addMapper(JmFavoriteSqliteMapper.class);
        config.addMapper(JmFavoriteAlbumSqliteMapper.class);
        MybatisPlusInterceptor pagination = new MybatisPlusInterceptor();
        pagination.addInnerInterceptor(new PaginationInnerInterceptor(DbType.SQLITE));
        config.addInterceptor(pagination);
        session = new MybatisSqlSessionFactoryBuilder().build(config).openSession(true);

        try (var statement = session.getConnection().createStatement()) {
            // 必须与 SqliteDatabaseInitMapper.xml 的 createJmAlbum 完全一致：
            // MyBatis-Plus 的 selectById 会 SELECT 实体全部列，缺列会直接报 no such column
            statement.execute("CREATE TABLE t_jm_album ("
                    + "`id` INTEGER NOT NULL PRIMARY KEY, `name` TEXT, `album_folder_name` TEXT, `images` TEXT,"
                    + "`add_time` TEXT, `description` TEXT, `total_views` TEXT, `likes` TEXT, `series` TEXT,"
                    + "`series_id` TEXT, `comment_total` TEXT, `author` TEXT, `tags` TEXT, `works` TEXT,"
                    + "`actors` TEXT, `related_list` TEXT, `liked` INTEGER, `is_favorite` INTEGER, `is_aids` INTEGER,"
                    + "`price` TEXT, `purchased` TEXT, `raw` TEXT, `collected` INTEGER DEFAULT 0,"
                    + "`create_time` DATETIME, `modify_time` DATETIME)");
            // 与 createJmFavorite / createJmFavoriteAlbum 保持一致
            statement.execute("CREATE TABLE t_jm_favorite ("
                    + "id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL,"
                    + "sort_order INTEGER DEFAULT 0, create_time DATETIME, modify_time DATETIME)");
            statement.execute("CREATE TABLE t_jm_favorite_album ("
                    + "id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, favorite_id INTEGER NOT NULL,"
                    + "album_id INTEGER NOT NULL, create_time DATETIME)");
            statement.execute("CREATE UNIQUE INDEX t_jm_favorite_name_idx ON t_jm_favorite (name)");
            statement.execute("CREATE UNIQUE INDEX t_jm_favorite_album_idx ON t_jm_favorite_album (favorite_id, album_id)");
        }

        service = new JmcomicSqliteServiceImpl();
        inject("jmAlbumSqliteMapper", session.getMapper(JmAlbumSqliteMapper.class));
        inject("jmFavoriteSqliteMapper", session.getMapper(JmFavoriteSqliteMapper.class));
        inject("jmFavoriteAlbumSqliteMapper", session.getMapper(JmFavoriteAlbumSqliteMapper.class));
    }

    @AfterEach
    void closeDatabase() {
        if (session != null) {
            session.close();
        }
    }

    private void inject(String fieldName, Object mapper) throws Exception {
        Field field = JmcomicSqliteServiceImpl.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(service, mapper);
    }

    private void insertAlbum(long id, String name) {
        JmAlbumSqlite album = new JmAlbumSqlite();
        album.setId(id);
        album.setName(name);
        album.setAuthor("[\"author\"]");
        album.setTags("[\"tag\"]");
        album.setCollected(false);
        album.setCreateTime("20260101000000");
        album.setModifyTime("20260101000000");
        session.getMapper(JmAlbumSqliteMapper.class).insert(album);
    }

    private JmAlbumSqlite loadAlbum(long id) {
        return session.getMapper(JmAlbumSqliteMapper.class).selectById(id);
    }

    private JmFavoriteResp favoriteByName(String name) {
        return service.listFavorites().stream()
                .filter(item -> name.equals(item.getName()))
                .findFirst()
                .orElse(null);
    }

    /**
     * 不指定收藏夹收藏时应自动落到默认收藏夹，并创建默认收藏夹
     */
    @Test
    void collectWithoutFavoriteFallsBackToDefaultFavorite() {
        insertAlbum(1001L, "album-a");
        JmAlbumCollectReq req = new JmAlbumCollectReq();
        req.setIds(List.of(1001L));
        req.setCollected(true);

        service.collectAlbums(req);

        List<JmFavoriteResp> favorites = service.listFavorites();
        assertEquals(1, favorites.size(), "应自动创建默认收藏夹");
        JmFavoriteResp defaultFavorite = favorites.getFirst();
        assertEquals(DEFAULT_NAME, defaultFavorite.getName());
        assertTrue(defaultFavorite.getIsDefault());
        assertEquals(1L, defaultFavorite.getAlbumCount());
        assertTrue(loadAlbum(1001L).getCollected(), "collected 应同步为 true");
    }

    /**
     * 按名称收藏时，名称不存在要直接新建该收藏夹
     */
    @Test
    void collectWithNewFavoriteNameCreatesFavorite() {
        insertAlbum(1002L, "album-b");
        JmAlbumCollectReq req = new JmAlbumCollectReq();
        req.setIds(List.of(1002L));
        req.setCollected(true);
        req.setFavoriteName("我的精选");

        service.collectAlbums(req);

        JmFavoriteResp created = favoriteByName("我的精选");
        assertNotNull(created, "应按名称新建收藏夹");
        assertFalse(created.getIsDefault());
        assertEquals(1L, created.getAlbumCount());
        assertTrue(loadAlbum(1002L).getCollected());
    }

    /**
     * 同一本漫画可以同时存在于多个收藏夹（多对多）
     */
    @Test
    void sameAlbumCanBelongToMultipleFavorites() {
        insertAlbum(1003L, "album-c");

        JmFavoriteAlbumReq first = new JmFavoriteAlbumReq();
        first.setAlbumIds(List.of(1003L));
        first.setFavoriteName("收藏夹A");
        service.addAlbumsToFavorite(first);

        JmFavoriteAlbumReq second = new JmFavoriteAlbumReq();
        second.setAlbumIds(List.of(1003L));
        second.setFavoriteName("收藏夹B");
        service.addAlbumsToFavorite(second);

        assertEquals(1L, favoriteByName("收藏夹A").getAlbumCount());
        assertEquals(1L, favoriteByName("收藏夹B").getAlbumCount());
        assertTrue(loadAlbum(1003L).getCollected());
    }

    /**
     * 重复加入同一收藏夹不应产生重复关联（唯一索引 + 去重插入）
     */
    @Test
    void addingSameAlbumTwiceToSameFavoriteIsIdempotent() {
        insertAlbum(1004L, "album-d");
        JmFavoriteAlbumReq req = new JmFavoriteAlbumReq();
        req.setAlbumIds(List.of(1004L, 1004L));
        req.setFavoriteName("去重夹");

        service.addAlbumsToFavorite(req);
        service.addAlbumsToFavorite(req);

        assertEquals(1L, favoriteByName("去重夹").getAlbumCount());
    }

    /**
     * 从某一收藏夹移出后，若仍在其他收藏夹中，collected 必须保持 true
     */
    @Test
    void removingFromOneFavoriteKeepsCollectedWhenStillInAnother() {
        insertAlbum(1005L, "album-e");
        JmFavoriteAlbumReq a = new JmFavoriteAlbumReq();
        a.setAlbumIds(List.of(1005L));
        a.setFavoriteName("甲");
        service.addAlbumsToFavorite(a);
        JmFavoriteAlbumReq b = new JmFavoriteAlbumReq();
        b.setAlbumIds(List.of(1005L));
        b.setFavoriteName("乙");
        service.addAlbumsToFavorite(b);

        JmFavoriteAlbumRemoveReq removeReq = new JmFavoriteAlbumRemoveReq();
        removeReq.setAlbumIds(List.of(1005L));
        removeReq.setFavoriteId(favoriteByName("甲").getId());
        service.removeAlbumsFromFavorite(removeReq);

        assertTrue(loadAlbum(1005L).getCollected(), "仍在乙收藏夹中，收藏标记不应被清掉");
        assertEquals(0L, favoriteByName("甲").getAlbumCount());
        assertEquals(1L, favoriteByName("乙").getAlbumCount());
    }

    /**
     * 取消收藏会把漫画从所有收藏夹移出，并清掉 collected
     */
    @Test
    void uncollectRemovesAlbumFromAllFavorites() {
        insertAlbum(1006L, "album-f");
        JmFavoriteAlbumReq a = new JmFavoriteAlbumReq();
        a.setAlbumIds(List.of(1006L));
        a.setFavoriteName("甲");
        service.addAlbumsToFavorite(a);
        JmFavoriteAlbumReq b = new JmFavoriteAlbumReq();
        b.setAlbumIds(List.of(1006L));
        b.setFavoriteName("乙");
        service.addAlbumsToFavorite(b);

        JmAlbumCollectReq uncollect = new JmAlbumCollectReq();
        uncollect.setIds(List.of(1006L));
        uncollect.setCollected(false);
        service.collectAlbums(uncollect);

        assertFalse(loadAlbum(1006L).getCollected());
        assertEquals(0L, favoriteByName("甲").getAlbumCount());
        assertEquals(0L, favoriteByName("乙").getAlbumCount());
    }

    /**
     * 删除收藏夹后，其中的漫画若不再属于任何收藏夹，应回落到默认收藏夹而不是被取消收藏
     */
    @Test
    void deletingFavoriteMovesOrphanAlbumsToDefaultFavorite() {
        insertAlbum(1007L, "album-g");
        JmFavoriteAlbumReq req = new JmFavoriteAlbumReq();
        req.setAlbumIds(List.of(1007L));
        req.setFavoriteName("待删除夹");
        service.addAlbumsToFavorite(req);

        Long targetId = favoriteByName("待删除夹").getId();
        service.deleteFavorite(targetId);

        assertEquals(null, favoriteByName("待删除夹"), "收藏夹应被删除");
        JmFavoriteResp defaultFavorite = favoriteByName(DEFAULT_NAME);
        assertNotNull(defaultFavorite, "应存在默认收藏夹");
        assertEquals(1L, defaultFavorite.getAlbumCount(), "漫画应回落到默认收藏夹");
        assertTrue(loadAlbum(1007L).getCollected(), "回落不应取消收藏");
    }

    /**
     * 默认收藏夹不允许删除或重命名
     */
    @Test
    void defaultFavoriteCannotBeDeletedOrRenamed() {
        insertAlbum(1008L, "album-h");
        JmAlbumCollectReq req = new JmAlbumCollectReq();
        req.setIds(List.of(1008L));
        req.setCollected(true);
        service.collectAlbums(req);

        Long defaultId = favoriteByName(DEFAULT_NAME).getId();
        assertThrows(BusinessException.class, () -> service.deleteFavorite(defaultId));
        assertThrows(BusinessException.class, () -> service.renameFavorite(defaultId, "改名试试"));
    }

    /**
     * 收藏夹名称重复时应拒绝新建
     */
    @Test
    void duplicateFavoriteNameIsRejected() {
        service.createFavorite("重复夹");
        assertThrows(BusinessException.class, () -> service.createFavorite("重复夹"));
        assertThrows(BusinessException.class, () -> service.createFavorite("  重复夹  "),
                "名称应 trim 后再判重");
    }

    /**
     * 空名称与超长名称应被拒绝
     */
    @Test
    void blankOrTooLongFavoriteNameIsRejected() {
        assertThrows(BusinessException.class, () -> service.createFavorite("   "));
        assertThrows(BusinessException.class, () -> service.createFavorite("x".repeat(51)));
    }

    /**
     * listFavorites 应统计各自的漫画数量，并标记默认收藏夹
     */
    @Test
    void listFavoritesReturnsCountsAndDefaultFlag() {
        insertAlbum(1009L, "album-i");
        insertAlbum(1010L, "album-j");
        JmFavoriteAlbumReq req = new JmFavoriteAlbumReq();
        req.setAlbumIds(List.of(1009L, 1010L));
        req.setFavoriteName("两本夹");
        service.addAlbumsToFavorite(req);
        JmFavoriteAlbumReq single = new JmFavoriteAlbumReq();
        single.setAlbumIds(List.of(1009L));
        single.setFavoriteName("一本夹");
        service.addAlbumsToFavorite(single);

        assertEquals(2L, favoriteByName("两本夹").getAlbumCount());
        assertEquals(1L, favoriteByName("一本夹").getAlbumCount());
    }
}
