package com.haruhi.botserver.administration.service;

import cn.hutool.core.text.StrFormatter;
import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelReader;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.read.metadata.ReadSheet;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import com.baomidou.dynamic.datasource.ds.ItemDataSource;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.haruhi.botserver.infrastructure.persistence.DataBaseConst;
import com.haruhi.botserver.infrastructure.persistence.SqlTypeEnum;
import com.haruhi.botserver.administration.model.SqlExecuteResult;
import com.haruhi.botserver.infrastructure.persistence.persistence.entity.SqliteSchema;
import com.haruhi.botserver.infrastructure.persistence.persistence.entity.TableInfoSqlite;
import com.haruhi.botserver.shared.error.BusinessException;
import com.haruhi.botserver.infrastructure.persistence.persistence.mapper.SqliteDatabaseInitMapper;
import com.haruhi.botserver.infrastructure.persistence.persistence.mapper.SqliteSchemaMapper;
import com.haruhi.botserver.infrastructure.excel.BatchInsertListener;
import com.haruhi.botserver.administration.model.DatabaseInfoNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import javax.sql.DataSource;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SqliteDatabaseService{

    private static final Map<String, Object> TABPE_LOCK_MAP = new ConcurrentHashMap<>();

    /**
     * 默认JM收藏夹名称。与 JmcomicSqliteServiceImpl.DEFAULT_FAVORITE_NAME 保持一致，
     * 该收藏夹不允许删除或重命名，删除漫画时若不在其他收藏夹会自动回落到这里。
     */
    public static final String DEFAULT_JM_FAVORITE_NAME = "默认收藏夹";

    public static Object getLock(String tableName) {
        return TABPE_LOCK_MAP.computeIfAbsent(tableName, k -> new Object());
    }

    public static void removeLock(String tableName) {
        TABPE_LOCK_MAP.remove(tableName);
    }

    @Autowired
    private SqliteDatabaseInitMapper sqliteDatabaseInitMapper;

    @Autowired
    private SqliteSchemaMapper sqliteSchemaMapper;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    private void firstInit(){
        try {
            tableInit();
        }catch (Exception e) {
            log.error("初始化数据库异常",e);
            throw e;
        }
    }

    public void tableInit(){
//        sqliteDatabaseInitMapper.createChatRecord(DataBaseConst.T_CHAT_RECORD);
//        sqliteDatabaseInitMapper.createIndexEnhance(DataBaseConst.T_CHAT_RECORD,
//                StrFormatter.format("{}_group_user_time_idx",DataBaseConst.T_CHAT_RECORD),
//                "group_id,user_id,time",
//                false);
//
//        sqliteDatabaseInitMapper.createChatRecordExtend(DataBaseConst.T_CHAT_RECORD_EXTEND);
//        sqliteDatabaseInitMapper.createIndex(DataBaseConst.T_CHAT_RECORD_EXTEND,"chat_record_id");

        sqliteDatabaseInitMapper.createChatRecordExtendV2(DataBaseConst.T_CHAT_RECORD_EXTEND_V2);
        this.addColumnIfNotExists(DataBaseConst.T_CHAT_RECORD_EXTEND_V2,"raw_ws_message_binary","BLOB",false,null);
        this.addColumnIfNotExists(DataBaseConst.T_CHAT_RECORD_EXTEND_V2,"self_id","INTEGER",false,null);
        this.addColumnIfNotExists(DataBaseConst.T_CHAT_RECORD_EXTEND_V2,"message_type","TEXT",false,null);

        sqliteDatabaseInitMapper.createIndexEnhance(DataBaseConst.T_CHAT_RECORD_EXTEND_V2,"chat_id_user_IDX","chat_record_id,user_id",false);

        sqliteDatabaseInitMapper.createPokeReply(DataBaseConst.T_POKE_REPLY);

        sqliteDatabaseInitMapper.createCustomReply(DataBaseConst.T_CUSTOM_REPLY);
        sqliteDatabaseInitMapper.createIndex(DataBaseConst.T_CUSTOM_REPLY,"regex");
        sqliteDatabaseInitMapper.createIndex(DataBaseConst.T_CUSTOM_REPLY,"cq_type");
        sqliteDatabaseInitMapper.createIndex(DataBaseConst.T_CUSTOM_REPLY,"is_text");
        sqliteDatabaseInitMapper.createIndex(DataBaseConst.T_CUSTOM_REPLY,"group_ids");
        sqliteDatabaseInitMapper.createIndex(DataBaseConst.T_CUSTOM_REPLY,"deleted");


        sqliteDatabaseInitMapper.createWordStrip(DataBaseConst.T_WORD_STRIP);
        sqliteDatabaseInitMapper.createIndexEnhance(DataBaseConst.T_WORD_STRIP,
                StrFormatter.format("{}_group_self_idx",DataBaseConst.T_WORD_STRIP),
                "group_id,self_id",
                false);


        sqliteDatabaseInitMapper.createPixiv(DataBaseConst.T_PIXIV);
        sqliteDatabaseInitMapper.createIndex(DataBaseConst.T_PIXIV,"img_url");
        sqliteDatabaseInitMapper.createIndex(DataBaseConst.T_PIXIV,"is_r18");
        sqliteDatabaseInitMapper.createIndex(DataBaseConst.T_PIXIV,"tags");

        sqliteDatabaseInitMapper.createJmAlbum(DataBaseConst.T_JM_ALBUM);
        this.addColumnIfNotExists(DataBaseConst.T_JM_ALBUM,"raw","TEXT",false,null);
        // 本地收藏标记，与JM服务器返回的 is_favorite 无关
        this.addColumnIfNotExists(DataBaseConst.T_JM_ALBUM,"collected","INTEGER",false,"0");
        sqliteDatabaseInitMapper.createIndex(DataBaseConst.T_JM_ALBUM,"name");
        sqliteDatabaseInitMapper.createIndex(DataBaseConst.T_JM_ALBUM,"tags");

        sqliteDatabaseInitMapper.createJmChapterImage(DataBaseConst.T_JM_CHAPTER_IMAGE);
        this.addColumnIfNotExists(DataBaseConst.T_JM_CHAPTER_IMAGE,"chapter_sort","TEXT",false,null);
        this.addColumnIfNotExists(DataBaseConst.T_JM_CHAPTER_IMAGE,"chapter_title","TEXT",false,null);
        sqliteDatabaseInitMapper.createIndexEnhance(DataBaseConst.T_JM_CHAPTER_IMAGE,
                StrFormatter.format("{}_album_chapter_file_idx",DataBaseConst.T_JM_CHAPTER_IMAGE),
                "album_id,chapter_id,image_file",
                true);
        sqliteDatabaseInitMapper.createIndexEnhance(DataBaseConst.T_JM_CHAPTER_IMAGE,
                StrFormatter.format("{}_album_chapter_sort_idx",DataBaseConst.T_JM_CHAPTER_IMAGE),
                "album_id,chapter_id,image_sort",
                false);

        // JM收藏夹：同一漫画可属于多个收藏夹（多对多）
        sqliteDatabaseInitMapper.createJmFavorite(DataBaseConst.T_JM_FAVORITE);
        sqliteDatabaseInitMapper.createIndexEnhance(DataBaseConst.T_JM_FAVORITE,
                StrFormatter.format("{}_name_idx",DataBaseConst.T_JM_FAVORITE),
                "name",
                true);
        sqliteDatabaseInitMapper.createJmFavoriteAlbum(DataBaseConst.T_JM_FAVORITE_ALBUM);
        // 同一个收藏夹内不允许重复添加同一本漫画
        sqliteDatabaseInitMapper.createIndexEnhance(DataBaseConst.T_JM_FAVORITE_ALBUM,
                StrFormatter.format("{}_favorite_album_idx",DataBaseConst.T_JM_FAVORITE_ALBUM),
                "favorite_id,album_id",
                true);
        sqliteDatabaseInitMapper.createIndexEnhance(DataBaseConst.T_JM_FAVORITE_ALBUM,
                StrFormatter.format("{}_album_idx",DataBaseConst.T_JM_FAVORITE_ALBUM),
                "album_id",
                false);
        this.initDefaultJmFavorite();

        sqliteDatabaseInitMapper.createSendLikeRecord(DataBaseConst.T_SEND_LIKE_RECORD);
        sqliteDatabaseInitMapper.createIndexEnhance(DataBaseConst.T_SEND_LIKE_RECORD,
                StrFormatter.format("{}_user_send_time_idx",DataBaseConst.T_SEND_LIKE_RECORD),
                "user_id,send_time",
                false);


        sqliteDatabaseInitMapper.createDictionary(DataBaseConst.T_DICTIONARY);
        this.addColumnIfNotExists(DataBaseConst.T_DICTIONARY,"remark","TEXT",false,null);
        sqliteDatabaseInitMapper.createIndex(DataBaseConst.T_DICTIONARY,"key");


        sqliteDatabaseInitMapper.createGroupInfo(DataBaseConst.T_GROUP_INFO);
        sqliteDatabaseInitMapper.createIndexEnhance(DataBaseConst.T_GROUP_INFO,
                StrFormatter.format("{}_group_self_idx",DataBaseConst.T_GROUP_INFO),
                "group_id,self_id",
                false);

        sqliteDatabaseInitMapper.createGroupMember(DataBaseConst.T_GROUP_MEMBER);
        sqliteDatabaseInitMapper.createIndexEnhance(DataBaseConst.T_GROUP_MEMBER,
                StrFormatter.format("{}_group_user_idx",DataBaseConst.T_GROUP_MEMBER),
                "group_id,user_id",
                false);
        sqliteDatabaseInitMapper.createIndexEnhance(DataBaseConst.T_GROUP_MEMBER,
                StrFormatter.format("{}_user_idx",DataBaseConst.T_GROUP_MEMBER),
                "user_id",
                false);

        sqliteDatabaseInitMapper.createFriend(DataBaseConst.T_FRIEND);
        sqliteDatabaseInitMapper.createIndexEnhance(DataBaseConst.T_FRIEND,
                StrFormatter.format("{}_user_self_idx",DataBaseConst.T_FRIEND),
                "user_id,self_id",
                false);

        sqliteDatabaseInitMapper.createSystemLog(DataBaseConst.T_SYSTEM_LOG);
        sqliteDatabaseInitMapper.createIndexEnhance(DataBaseConst.T_SYSTEM_LOG,
                StrFormatter.format("{}_module_time_idx",DataBaseConst.T_SYSTEM_LOG),
                "business_module,create_time",
                false);
        sqliteDatabaseInitMapper.createIndexEnhance(DataBaseConst.T_SYSTEM_LOG,
                StrFormatter.format("{}_level_time_idx",DataBaseConst.T_SYSTEM_LOG),
                "level,create_time",
                false);
        sqliteDatabaseInitMapper.createIndexEnhance(DataBaseConst.T_SYSTEM_LOG,
                StrFormatter.format("{}_trace_idx",DataBaseConst.T_SYSTEM_LOG),
                "trace_id",
                false);

        sqliteDatabaseInitMapper.createBilibiliSubscribe(DataBaseConst.T_BILIBILI_SUBSCRIBE);
        this.addColumnIfNotExists(DataBaseConst.T_BILIBILI_SUBSCRIBE,"uname","TEXT",false,null);
        this.addColumnIfNotExists(DataBaseConst.T_BILIBILI_SUBSCRIBE,"face","TEXT",false,null);
        this.addColumnIfNotExists(DataBaseConst.T_BILIBILI_SUBSCRIBE,"room_id","INTEGER",false,null);
        this.addColumnIfNotExists(DataBaseConst.T_BILIBILI_SUBSCRIBE,"at_all_group_ids","TEXT",false,null);
        sqliteDatabaseInitMapper.createIndexEnhance(DataBaseConst.T_BILIBILI_SUBSCRIBE,
                StrFormatter.format("{}_uid_update_time_idx",DataBaseConst.T_BILIBILI_SUBSCRIBE),
                "uid,update_time",
                false);

        // b站视频信息：bvid+cid 组合唯一，由 getVideoDetail 自动入库
        sqliteDatabaseInitMapper.createBilibiliVideo(DataBaseConst.T_BILIBILI_VIDEO);
        sqliteDatabaseInitMapper.createIndexEnhance(DataBaseConst.T_BILIBILI_VIDEO,
                StrFormatter.format("{}_bvid_cid_idx",DataBaseConst.T_BILIBILI_VIDEO),
                "bvid,cid",
                true);
        sqliteDatabaseInitMapper.createIndexEnhance(DataBaseConst.T_BILIBILI_VIDEO,
                StrFormatter.format("{}_owner_mid_idx",DataBaseConst.T_BILIBILI_VIDEO),
                "owner_mid",
                false);
        sqliteDatabaseInitMapper.createIndexEnhance(DataBaseConst.T_BILIBILI_VIDEO,
                StrFormatter.format("{}_pubdate_idx",DataBaseConst.T_BILIBILI_VIDEO),
                "pubdate",
                false);
    }

    private static final ConcurrentHashMap<String,Boolean> tableExistsCache = new ConcurrentHashMap<>();

    public boolean checkTableExists(String tableName) {
        if (StringUtils.isBlank(tableName)) {
            return false;
        }
        Boolean b = tableExistsCache.get(tableName);
        if (b != null) {
            return b;
        }
        Long count = sqliteSchemaMapper.selectCount(new LambdaQueryWrapper<SqliteSchema>()
                .eq(SqliteSchema::getType, DatabaseInfoNode.TYPE_TABLE)
                .eq(SqliteSchema::getTblName, tableName));

        boolean b1 = Objects.nonNull(count) && count > 0;
        if (b1) {
            tableExistsCache.put(tableName, true);
        }
        return b1;

    }

    public String getChatTableName(Long groupId, Long selfId) {
        if (Objects.nonNull(groupId)) {
            return DataBaseConst.T_CHAT_RECORD_GROUP_PREFIX + groupId;
        }
        if (Objects.nonNull(selfId)) {
            return DataBaseConst.T_CHAT_RECORD_PRIVATE_PREFIX + selfId;
        }
        return null;
    }

    public int createChatRecordPrivateIfNotExists(Long selfId){
        if (Objects.isNull(selfId)) {
            return 0;
        }

        String tableName = this.getChatTableName(null, selfId);
        if(checkTableExists(tableName)){
            return 0;
        }

        Object lock = getLock(tableName);
        try {
            synchronized (lock) {
                if(checkTableExists(tableName)){
                    return 0;
                }

                sqliteDatabaseInitMapper.createChatRecordPrivate(tableName);
//                sqliteDatabaseInitMapper.createIndexEnhance(tableName, "idx_user_time_"+selfId,"user_id,time",false);
                sqliteDatabaseInitMapper.createIndexEnhance(tableName, "idx_target_time_"+selfId,"target_id,time",false);
                return 1;
            }
        }finally {
            removeLock(tableName);
        }
    }

    public int createChatRecordGroupIfNotExists(Long groupId){
        if (Objects.isNull(groupId)) {
            return 0;
        }

        String tableName = this.getChatTableName(groupId, null);
        if(checkTableExists(tableName)){
            return 0;
        }

        Object lock = getLock(tableName);
        try {
            synchronized (lock) {
                if(checkTableExists(tableName)){
                    return 0;
                }

                sqliteDatabaseInitMapper.createChatRecordGroup(tableName);
                sqliteDatabaseInitMapper.createIndexEnhance(tableName, "idx_user_time_"+groupId,"user_id,time",false);
                return 1;
            }
        }finally {
            removeLock(tableName);
        }
    }


    public int addColumnIfNotExists(String tableName, String columnName, String columnType,boolean notNull,String defaultValue) {
        List<TableInfoSqlite> tableInfo = sqliteDatabaseInitMapper.pragmaTableInfo(tableName);
        if (CollectionUtils.isEmpty(tableInfo)) {
            return 0;
        }
        if (tableInfo.stream().map(TableInfoSqlite::getName).toList().contains(columnName)) {
            return 0;
        }
        return sqliteDatabaseInitMapper.addColumn(tableName,columnName,columnType,notNull,defaultValue);
    }

    /**
     * 初始化默认收藏夹。
     * name 上有唯一索引，用 INSERT OR IGNORE 保证幂等：已存在时不报错也不重复插入。
     * 默认收藏夹不允许删除/重命名，具体约束在 JmcomicSqliteServiceImpl 里校验。
     */
    private void initDefaultJmFavorite() {
        try {
            jdbcTemplate.update("INSERT OR IGNORE INTO `" + DataBaseConst.T_JM_FAVORITE
                    + "` (`name`, `sort_order`) VALUES (?, ?)", DEFAULT_JM_FAVORITE_NAME, 0);
        } catch (Exception e) {
            log.warn("初始化默认JM收藏夹失败: {}", e.getMessage());
        }
    }

    public List<SqlExecuteResult> executeSql(String sql) {
        DruidDataSource masterDataSource = getMasterDataSource();
        return executeSql(sql, masterDataSource.getUrl());
    }

    public List<SqlExecuteResult> executeSql(String sql, String url) {
        List<String> sqls = handleSql(sql);
        if (CollectionUtils.isEmpty(sqls)) {
            SqlExecuteResult sqlExecuteResult = new SqlExecuteResult();
            sqlExecuteResult.setType(SqlTypeEnum.ERROR.name());
            sqlExecuteResult.setErrorMessage("无SQL");
            return Collections.singletonList(sqlExecuteResult);
        }

        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement()){

            List<SqlExecuteResult> results = new ArrayList<>();
            for (String s : sqls) {
                SqlExecuteResult executeResult = new SqlExecuteResult();
                executeResult.setSql(s);
                try {
                    long l = System.currentTimeMillis();
                    boolean hasResultSet = stmt.execute(s);
                    executeResult.setCost(System.currentTimeMillis() - l);
                    if (hasResultSet) {
                        try (ResultSet rs = stmt.getResultSet()){
                            List<List<Object>> data = convertResultSetToList(rs);
                            executeResult.setType(SqlTypeEnum.QUERY.name());
                            executeResult.setData(data);
                        }
                    } else {
                        int affectedRows = stmt.getUpdateCount();
                        executeResult.setData(affectedRows);
                        if (affectedRows == -1) {
                            executeResult.setType(SqlTypeEnum.DDL.name());
                        } else {
                            executeResult.setType(SqlTypeEnum.UPDATE.name());
                        }
                    }
                }catch (SQLException e) {
                    executeResult.setType(SqlTypeEnum.ERROR.name());
                    executeResult.setErrorMessage(e.getMessage());
                    log.error("SQL Execute Error", e);
                }
                results.add(executeResult);
            }
            return results;
        } catch (SQLException e) {
            SqlExecuteResult sqlExecuteResult = new SqlExecuteResult();
            sqlExecuteResult.setSql(sql);
            sqlExecuteResult.setType(SqlTypeEnum.ERROR.name());
            sqlExecuteResult.setErrorMessage(e.getMessage());
            log.error("打开连接异常", e);
            return new ArrayList<>(Collections.singletonList(sqlExecuteResult));
        }
    }

    private List<String> handleSql(String originalSql) {
        if (StringUtils.isBlank(originalSql)) {
            return null;
        }
        originalSql = originalSql.replaceAll("--.*?(\\R|$)", "");
        String[] split = originalSql.split("(?<=;)");
        return Arrays.stream(split).filter(StringUtils::isNotBlank).map(String::trim).collect(Collectors.toList());
    }

    /**
     * 第一行为 字段列表
     * @param rs
     * @return
     * @throws SQLException
     */
    private List<List<Object>> convertResultSetToList(ResultSet rs) throws SQLException {
        List<List<Object>> result = new ArrayList<>();
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();
        // 添加列名
        List<Object> columnNames = new ArrayList<>();
        for (int i = 1; i <= columnCount; i++) {
            columnNames.add(metaData.getColumnName(i));
        }
        result.add(columnNames);

        while (rs.next()) {
            List<Object> row = new ArrayList<>();
            for (int i = 1; i <= columnCount; i++) {
                row.add(rs.getObject(i));
            }
            result.add(row);
        }
        return result;
    }


    public DruidDataSource getMasterDataSource() {
        if (dataSource instanceof DynamicRoutingDataSource) {
            DynamicRoutingDataSource dynamicRoutingDataSource = (DynamicRoutingDataSource) dataSource;
            DataSource realDatasource = dynamicRoutingDataSource.getDataSource(DataBaseConst.DATA_SOURCE_MASTER);
            if (realDatasource instanceof ItemDataSource) {
                ItemDataSource itemDataSource = (ItemDataSource) realDatasource;
                if (itemDataSource.getDataSource() instanceof DruidDataSource) {
                    return (DruidDataSource) itemDataSource.getDataSource();
                }
            }

        }
        return null;
    }


    public void executeAndExport(String sql, OutputStream outputStream) {
        List<SqlExecuteResult> results = executeSql(sql);
        if (CollectionUtils.isEmpty(results)) {
            throw new BusinessException("无执行结果");
        }
        exportResult(results, outputStream);
    }

    public void exportResult(List<SqlExecuteResult> queryResult, OutputStream outputStream) {
        queryResult = CollectionUtils.isNotEmpty(queryResult) ? queryResult.stream()
                .filter(e -> SqlTypeEnum.QUERY.name().equals(e.getType()))
                .collect(Collectors.toList())
                : null;
        if (CollectionUtils.isEmpty(queryResult)) {
            throw new BusinessException("无查询结果");
        }
        ExcelWriter excelWriter = EasyExcel.write(outputStream)
                .useDefaultStyle(false)
                .build();
        try {

            for (int i = 0; i < queryResult.size(); i++) {
                SqlExecuteResult result = queryResult.get(i);
                List<List<Object>> excelData = (List<List<Object>>)result.getData();

                List<List<String>> head = new ArrayList<>();
                for (Object header : excelData.getFirst()) {
                    head.add(Collections.singletonList(header == null ? "" : header.toString()));
                }
                WriteSheet sheet = EasyExcel.writerSheet(i, i+"-sheet")
                        .head(head)
                        .build();
                List<List<Object>> dataRows = excelData.subList(1, excelData.size());
                excelWriter.write(dataRows, sheet);
            }
        }finally {
            excelWriter.finish();
        }
    }

    public void importData(InputStream inputStream, String tableName) {

        ExcelReader excelReader = null;
        try {
            excelReader = EasyExcel.read(inputStream).build();
            BatchInsertListener batchInsertListener = new BatchInsertListener(jdbcTemplate, tableName);
            ReadSheet sheet = EasyExcel.readSheet(0)
                    .registerReadListener(batchInsertListener).build();
            excelReader.read(sheet);
        }finally {
            if (excelReader != null) {
                excelReader.finish();
            }
        }

    }

}
