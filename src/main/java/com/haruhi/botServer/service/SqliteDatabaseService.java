package com.haruhi.botServer.service;

import com.haruhi.botServer.config.DataBaseConfig;
import com.haruhi.botServer.mapper.sqlite.SqliteDatabaseInitMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

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

        sqliteDatabaseInitMapper.createWordStrip(DataBaseConfig.T_WORD_STRIP);

        sqliteDatabaseInitMapper.createPixiv(DataBaseConfig.T_PIXIV);
        sqliteDatabaseInitMapper.createIndex(DataBaseConfig.T_PIXIV,"img_url");
        sqliteDatabaseInitMapper.createIndex(DataBaseConfig.T_PIXIV,"is_r18");
        sqliteDatabaseInitMapper.createIndex(DataBaseConfig.T_PIXIV,"tags");

        sqliteDatabaseInitMapper.createSendLikeRecord(DataBaseConfig.T_SEND_LIKE_RECORD);

        sqliteDatabaseInitMapper.createDictionary(DataBaseConfig.T_DICTIONARY);
        sqliteDatabaseInitMapper.createIndex(DataBaseConfig.T_DICTIONARY,"key");
    }

}
