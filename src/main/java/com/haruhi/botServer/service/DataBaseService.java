package com.haruhi.botServer.service;

import com.haruhi.botServer.config.DataBaseConfig;
import com.haruhi.botServer.mapper.TableInitMapper;
import com.haruhi.botServer.mapper.system.DataBaseInitMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class DataBaseService {

    @Autowired
    private DataBaseInitMapper dataBaseInitMapper;
    @Autowired
    private TableInitMapper tableInitMapper;


    public synchronized void initDataBase(){
        log.info("开始初始化数据库...");
        try {
            if (dataBaseInitMapper.tableIsExist(DataBaseConfig.DATA_BASE_BOT,DataBaseConfig.T_GROUP_CHAT_HISTORY) == 0) {
                tableInitMapper.createGroupChatHistory(DataBaseConfig.T_GROUP_CHAT_HISTORY);
            }

            log.info("初始化数据库完成");
        }catch (Exception e){
            log.error("初始化数据库异常",e);
            System.exit(0);
        }
    }

}
