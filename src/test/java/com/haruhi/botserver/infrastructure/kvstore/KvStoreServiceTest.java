package com.haruhi.botserver.infrastructure.kvstore;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder;
import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.haruhi.botserver.infrastructure.cache.SqlCacheStore;
import com.haruhi.botserver.administration.controller.DictionaryController;
import com.haruhi.botserver.infrastructure.kvstore.model.KvQuery;
import com.haruhi.botserver.infrastructure.kvstore.persistence.entity.KvEntry;
import com.haruhi.botserver.infrastructure.kvstore.persistence.mapper.KvEntryMapper;
import com.haruhi.botserver.infrastructure.kvstore.service.KvStoreService;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import static org.junit.jupiter.api.Assertions.*;

class KvStoreServiceTest {
    private SqlSession session;
    private KvStoreService store;

    @BeforeEach
    void openDatabase() throws Exception {
        MybatisConfiguration config = new MybatisConfiguration();
        config.setEnvironment(new Environment("test", new JdbcTransactionFactory(),
                new UnpooledDataSource("org.sqlite.JDBC", "jdbc:sqlite::memory:", null)));
        config.addMapper(KvEntryMapper.class);
        MybatisPlusInterceptor pagination = new MybatisPlusInterceptor();
        pagination.addInnerInterceptor(new PaginationInnerInterceptor(DbType.SQLITE));
        config.addInterceptor(pagination);
        session = new MybatisSqlSessionFactoryBuilder().build(config).openSession(true);
        try (var statement = session.getConnection().createStatement()) {
            statement.execute("CREATE TABLE t_dictionary (id INTEGER PRIMARY KEY AUTOINCREMENT, key TEXT NOT NULL, content TEXT NOT NULL, remark TEXT, create_time DATETIME, modify_time DATETIME)");
        }
        store = new KvStoreService(session.getMapper(KvEntryMapper.class));
    }

    @AfterEach
    void closeDatabase() { if (session != null) session.close(); }

    @Test
    void insertAndOverwriteKeepExistingRowAndRemark() {
        store.put("sample", "first", "description");
        Long id = store.getOne("sample").getId();
        store.put("sample", "second", "replacement description");
        assertEquals("second", store.get("sample"));
        assertEquals(id, store.getOne("sample").getId());
        assertEquals("description", store.getOne("sample").getRemark());
        assertEquals(1, store.getList("sample").size());
    }

    @Test
    void managementCrudUsesSameStore() {
        KvEntry entry = new KvEntry();
        entry.setKey("managed");
        entry.setContent("one");
        assertEquals(1, store.add(entry));
        entry.setContent("two");
        assertEquals(1, store.update(entry));
        KvQuery query = new KvQuery();
        query.setKey("managed");
        assertEquals("two", store.search(query, false).getRecords().getFirst().getContent());
        assertEquals(1, store.deleteBatch(java.util.List.of(entry)));
        assertNull(store.get("managed"));
    }

    @Test
    void sqlEditorUsesStoreWithoutChangingItsKeyOrNullSemantics() {
        SqlCacheStore sql = new SqlCacheStore(store);
        assertNull(sql.get());
        assertFalse(sql.shouldSave(""));
        sql.put("select 1");
        assertEquals("select 1", store.get("db.sql_cache"));
        assertTrue(sql.shouldSave(""));
        sql.put(null);
        assertEquals("", sql.get());
        assertNotNull(store.getOne("db.sql_cache").getRemark());
    }

    @Test
    void explicitRefreshRetainsMultipleValuesPerKey() {
        store.add("items", "1");
        store.add("items", "2");
        store.refreshCache();
        assertEquals(2, store.getValues("items").size());
        assertNotNull(store.getInCache("items", null));
        assertEquals("fallback", store.getInCache("missing", "fallback"));
        KvStoreService separateInstance = new KvStoreService(session.getMapper(KvEntryMapper.class));
        assertEquals("unloaded", separateInstance.getInCache("items", "unloaded"));
    }

    @Test
    void existingHttpRouteAndJsonFieldsRemainCompatible() throws Exception {
        var mvc = MockMvcBuilders.standaloneSetup(new DictionaryController(store)).build();
        mvc.perform(post("/api/dict/add").contentType("application/json")
                        .content("{\"key\":\"http\",\"content\":\"value\",\"remark\":\"note\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.key").value("http"))
                .andExpect(jsonPath("$.data.content").value("value"));
        mvc.perform(post("/api/dict/search").contentType("application/json")
                        .content("{\"key\":\"http\",\"currentPage\":1,\"pageSize\":10}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].remark").value("note"));
    }
}
