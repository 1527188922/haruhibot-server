package com.haruhi.botServer.service;

import com.alibaba.druid.pool.DruidDataSource;
import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import com.baomidou.dynamic.datasource.ds.ItemDataSource;
import com.haruhi.botServer.constant.DataBaseConst;
import com.haruhi.botServer.constant.SqlTypeEnum;
import com.haruhi.botServer.dto.SqlExecuteResult;
import com.haruhi.botServer.entity.TableInfoSqlite;
import com.haruhi.botServer.mapper.SqliteDatabaseInitMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SqliteDatabaseService{

    @Autowired
    private SqliteDatabaseInitMapper sqliteDatabaseInitMapper;
    @Autowired
    private DataSource dataSource;

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
        sqliteDatabaseInitMapper.createChatRecord(DataBaseConst.T_CHAT_RECORD);
        sqliteDatabaseInitMapper.createIndex(DataBaseConst.T_CHAT_RECORD,"content");
        sqliteDatabaseInitMapper.createIndex(DataBaseConst.T_CHAT_RECORD,"self_id");
        sqliteDatabaseInitMapper.createIndex(DataBaseConst.T_CHAT_RECORD,"user_id");
        sqliteDatabaseInitMapper.createIndex(DataBaseConst.T_CHAT_RECORD,"group_id");
        sqliteDatabaseInitMapper.createIndex(DataBaseConst.T_CHAT_RECORD,"time");
        sqliteDatabaseInitMapper.createIndex(DataBaseConst.T_CHAT_RECORD,"card");
        sqliteDatabaseInitMapper.createIndex(DataBaseConst.T_CHAT_RECORD,"nickname");
        sqliteDatabaseInitMapper.createIndex(DataBaseConst.T_CHAT_RECORD,"message_type");

        sqliteDatabaseInitMapper.createChatRecordExtend(DataBaseConst.T_CHAT_RECORD_EXTEND);
        sqliteDatabaseInitMapper.createIndex(DataBaseConst.T_CHAT_RECORD_EXTEND,"chat_record_id");
        sqliteDatabaseInitMapper.createIndex(DataBaseConst.T_CHAT_RECORD_EXTEND,"message_id");
        sqliteDatabaseInitMapper.createIndex(DataBaseConst.T_CHAT_RECORD_EXTEND,"time");

        sqliteDatabaseInitMapper.createPokeReply(DataBaseConst.T_POKE_REPLY);

        sqliteDatabaseInitMapper.createCustomReply(DataBaseConst.T_CUSTOM_REPLY);
        sqliteDatabaseInitMapper.createIndex(DataBaseConst.T_CUSTOM_REPLY,"regex");
        sqliteDatabaseInitMapper.createIndex(DataBaseConst.T_CUSTOM_REPLY,"cq_type");
        sqliteDatabaseInitMapper.createIndex(DataBaseConst.T_CUSTOM_REPLY,"is_text");
        sqliteDatabaseInitMapper.createIndex(DataBaseConst.T_CUSTOM_REPLY,"group_ids");
        sqliteDatabaseInitMapper.createIndex(DataBaseConst.T_CUSTOM_REPLY,"deleted");


        sqliteDatabaseInitMapper.createWordStrip(DataBaseConst.T_WORD_STRIP);
        sqliteDatabaseInitMapper.createIndex(DataBaseConst.T_WORD_STRIP,"user_id");
        sqliteDatabaseInitMapper.createIndex(DataBaseConst.T_WORD_STRIP,"group_id");
        sqliteDatabaseInitMapper.createIndex(DataBaseConst.T_WORD_STRIP,"key_word");
        sqliteDatabaseInitMapper.createIndex(DataBaseConst.T_WORD_STRIP,"create_time");
        sqliteDatabaseInitMapper.createIndex(DataBaseConst.T_WORD_STRIP,"modify_time");


        sqliteDatabaseInitMapper.createPixiv(DataBaseConst.T_PIXIV);
        sqliteDatabaseInitMapper.createIndex(DataBaseConst.T_PIXIV,"img_url");
        sqliteDatabaseInitMapper.createIndex(DataBaseConst.T_PIXIV,"is_r18");
        sqliteDatabaseInitMapper.createIndex(DataBaseConst.T_PIXIV,"tags");

        sqliteDatabaseInitMapper.createSendLikeRecord(DataBaseConst.T_SEND_LIKE_RECORD);
        sqliteDatabaseInitMapper.createIndex(DataBaseConst.T_SEND_LIKE_RECORD,"message_type");
        sqliteDatabaseInitMapper.createIndex(DataBaseConst.T_SEND_LIKE_RECORD,"self_id");
        sqliteDatabaseInitMapper.createIndex(DataBaseConst.T_SEND_LIKE_RECORD,"user_id");
        sqliteDatabaseInitMapper.createIndex(DataBaseConst.T_SEND_LIKE_RECORD,"send_time");


        sqliteDatabaseInitMapper.createDictionary(DataBaseConst.T_DICTIONARY);
        addColumnIfNotExists(DataBaseConst.T_DICTIONARY,"remark","TEXT",false,null);
        sqliteDatabaseInitMapper.createIndex(DataBaseConst.T_DICTIONARY,"key");
        sqliteDatabaseInitMapper.createIndex(DataBaseConst.T_DICTIONARY,"content");


        sqliteDatabaseInitMapper.createGroupInfo(DataBaseConst.T_GROUP_INFO);
        sqliteDatabaseInitMapper.createIndex(DataBaseConst.T_GROUP_INFO,"self_id");
        sqliteDatabaseInitMapper.createIndex(DataBaseConst.T_GROUP_INFO,"group_id");
        sqliteDatabaseInitMapper.createIndex(DataBaseConst.T_GROUP_INFO,"group_name");
    }

    public int addColumnIfNotExists(String tableName, String columnName, String columnType,boolean notNull,String defaultValue) {
        List<TableInfoSqlite> tableInfo = sqliteDatabaseInitMapper.pragmaTableInfo(tableName);
        if (CollectionUtils.isEmpty(tableInfo)) {
            return 0;
        }
        if (tableInfo.stream().map(TableInfoSqlite::getName).collect(Collectors.toList()).contains(columnName)) {
            return 0;
        }
        return sqliteDatabaseInitMapper.addColumn(tableName,columnName,columnType,notNull,defaultValue);
    }

    public List<SqlExecuteResult> executeSql(String sql) {
        String url = getMasterDataSourceProperty();
        return executeSql(sql, url);
    }

    public List<SqlExecuteResult> executeSql(String sql, String url) {
        List<String> sqls = handleSql(sql);
        if (CollectionUtils.isEmpty(sqls)) {
            SqlExecuteResult sqlExecuteResult = new SqlExecuteResult();
            sqlExecuteResult.setType(SqlTypeEnum.ERROR.name());
            sqlExecuteResult.setErrorMessage("无SQL");
            return Collections.singletonList(sqlExecuteResult);
        }
        long l = System.currentTimeMillis();
        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement()){

            List<SqlExecuteResult> results = new ArrayList<>();
            for (String s : sqls) {
                SqlExecuteResult executeResult = new SqlExecuteResult();
                executeResult.setSql(s);
                try {
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
                        if (affectedRows == -1) {
                            executeResult.setType(SqlTypeEnum.DDL.name());
                        } else {
                            executeResult.setType(SqlTypeEnum.UPDATE.name());
                            executeResult.setData(affectedRows);
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


    private String getMasterDataSourceProperty() {
        if (dataSource instanceof DynamicRoutingDataSource) {
            DynamicRoutingDataSource dynamicRoutingDataSource = (DynamicRoutingDataSource) dataSource;
            DataSource realDatasource = dynamicRoutingDataSource.getDataSource(DataBaseConst.DATA_SOURCE_MASTER);
            if (realDatasource instanceof ItemDataSource) {
                ItemDataSource itemDataSource = (ItemDataSource) realDatasource;
                if (itemDataSource.getDataSource() instanceof DruidDataSource) {
                    DruidDataSource druidDataSource = (DruidDataSource) itemDataSource.getDataSource();
                    return druidDataSource.getUrl();
                }
            }

        }
        return null;
    }



}
