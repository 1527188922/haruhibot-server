package com.haruhi.botServer.service;

import com.haruhi.botServer.config.DataBaseConfig;
import com.haruhi.botServer.entity.TableInfoSqlite;
import com.haruhi.botServer.mapper.SqliteDatabaseInitMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SqliteDatabaseService{

    @Autowired
    private SqliteDatabaseInitMapper sqliteDatabaseInitMapper;

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
        sqliteDatabaseInitMapper.createChatRecord(DataBaseConfig.T_CHAT_RECORD);
        sqliteDatabaseInitMapper.createIndex(DataBaseConfig.T_CHAT_RECORD,"content");
        sqliteDatabaseInitMapper.createIndex(DataBaseConfig.T_CHAT_RECORD,"self_id");
        sqliteDatabaseInitMapper.createIndex(DataBaseConfig.T_CHAT_RECORD,"user_id");
        sqliteDatabaseInitMapper.createIndex(DataBaseConfig.T_CHAT_RECORD,"group_id");
        sqliteDatabaseInitMapper.createIndex(DataBaseConfig.T_CHAT_RECORD,"time");
        sqliteDatabaseInitMapper.createIndex(DataBaseConfig.T_CHAT_RECORD,"card");
        sqliteDatabaseInitMapper.createIndex(DataBaseConfig.T_CHAT_RECORD,"nickname");
        sqliteDatabaseInitMapper.createIndex(DataBaseConfig.T_CHAT_RECORD,"message_type");

        sqliteDatabaseInitMapper.createPokeReply(DataBaseConfig.T_POKE_REPLY);

        sqliteDatabaseInitMapper.createCustomReply(DataBaseConfig.T_CUSTOM_REPLY);
        sqliteDatabaseInitMapper.createIndex(DataBaseConfig.T_CUSTOM_REPLY,"regex");
        sqliteDatabaseInitMapper.createIndex(DataBaseConfig.T_CUSTOM_REPLY,"cq_type");
        sqliteDatabaseInitMapper.createIndex(DataBaseConfig.T_CUSTOM_REPLY,"is_text");
        sqliteDatabaseInitMapper.createIndex(DataBaseConfig.T_CUSTOM_REPLY,"group_ids");
        sqliteDatabaseInitMapper.createIndex(DataBaseConfig.T_CUSTOM_REPLY,"deleted");


        sqliteDatabaseInitMapper.createWordStrip(DataBaseConfig.T_WORD_STRIP);
        sqliteDatabaseInitMapper.createIndex(DataBaseConfig.T_WORD_STRIP,"user_id");
        sqliteDatabaseInitMapper.createIndex(DataBaseConfig.T_WORD_STRIP,"group_id");
        sqliteDatabaseInitMapper.createIndex(DataBaseConfig.T_WORD_STRIP,"key_word");
        sqliteDatabaseInitMapper.createIndex(DataBaseConfig.T_WORD_STRIP,"create_time");
        sqliteDatabaseInitMapper.createIndex(DataBaseConfig.T_WORD_STRIP,"modify_time");


        sqliteDatabaseInitMapper.createPixiv(DataBaseConfig.T_PIXIV);
        sqliteDatabaseInitMapper.createIndex(DataBaseConfig.T_PIXIV,"img_url");
        sqliteDatabaseInitMapper.createIndex(DataBaseConfig.T_PIXIV,"is_r18");
        sqliteDatabaseInitMapper.createIndex(DataBaseConfig.T_PIXIV,"tags");

        sqliteDatabaseInitMapper.createSendLikeRecord(DataBaseConfig.T_SEND_LIKE_RECORD);
        sqliteDatabaseInitMapper.createIndex(DataBaseConfig.T_SEND_LIKE_RECORD,"message_type");
        sqliteDatabaseInitMapper.createIndex(DataBaseConfig.T_SEND_LIKE_RECORD,"self_id");
        sqliteDatabaseInitMapper.createIndex(DataBaseConfig.T_SEND_LIKE_RECORD,"user_id");
        sqliteDatabaseInitMapper.createIndex(DataBaseConfig.T_SEND_LIKE_RECORD,"send_time");


        sqliteDatabaseInitMapper.createDictionary(DataBaseConfig.T_DICTIONARY);
        addColumnIfNotExists(DataBaseConfig.T_DICTIONARY,"remark","TEXT",false,null);
        sqliteDatabaseInitMapper.createIndex(DataBaseConfig.T_DICTIONARY,"key");
        sqliteDatabaseInitMapper.createIndex(DataBaseConfig.T_DICTIONARY,"content");


        sqliteDatabaseInitMapper.createGroupInfo(DataBaseConfig.T_GROUP_INFO);
        sqliteDatabaseInitMapper.createIndex(DataBaseConfig.T_GROUP_INFO,"self_id");
        sqliteDatabaseInitMapper.createIndex(DataBaseConfig.T_GROUP_INFO,"group_id");
        sqliteDatabaseInitMapper.createIndex(DataBaseConfig.T_GROUP_INFO,"group_name");
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

}
