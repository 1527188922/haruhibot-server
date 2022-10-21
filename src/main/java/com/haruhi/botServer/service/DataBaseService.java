package com.haruhi.botServer.service;

import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import com.baomidou.dynamic.datasource.spring.boot.autoconfigure.DataSourceProperty;
import com.baomidou.dynamic.datasource.spring.boot.autoconfigure.DynamicDataSourceProperties;
import com.baomidou.dynamic.datasource.spring.boot.autoconfigure.druid.DruidConfig;
import com.haruhi.botServer.config.DataBaseConfig;
import com.haruhi.botServer.mapper.TableInitMapper;
import com.haruhi.botServer.mapper.system.DataBaseInitMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class DataBaseService {

    @Autowired
    private DataBaseInitMapper dataBaseInitMapper;
    @Autowired
    private TableInitMapper tableInitMapper;
    @Autowired
    private DynamicDataSourceProperties dynamicDataSourceProperties;
    @Autowired
    private DynamicRoutingDataSource dynamicRoutingDataSource;


    public synchronized void initDataBase(){
        log.info("开始初始化数据库...");
        try {
            if(dataBaseInitMapper.dataBaseIsExist(DataBaseConfig.DATA_BASE_BOT) == 0){
                log.info("数据库不存在,开始创建:{}",DataBaseConfig.DATA_BASE_BOT);
                dataBaseInitMapper.createDataBase(DataBaseConfig.DATA_BASE_BOT);
                log.info("数据库创建成功");
            }

            addDataSource();

            if (dataBaseInitMapper.tableIsExist(DataBaseConfig.DATA_BASE_BOT,DataBaseConfig.T_GROUP_CHAT_HISTORY) == 0) {
                tableInitMapper.createGroupChatHistory(DataBaseConfig.T_GROUP_CHAT_HISTORY);
            }

            log.info("初始化数据库完成");
        }catch (Exception e){
            log.error("初始化数据库异常",e);
            System.exit(0);
        }
    }

   public void addDataSource(String name,DataSourceProperty dataSourceProperty){
       if(Strings.isBlank(name) || dataSourceProperty == null || Strings.isBlank(dataSourceProperty.getUsername())
               || Strings.isBlank(dataSourceProperty.getPassword()) || Strings.isBlank(dataSourceProperty.getUrl())
               || Strings.isBlank(dataSourceProperty.getDriverClassName())){
           throw new IllegalArgumentException();
       }
       DataSourceProperty dataSourceItem = dynamicDataSourceProperties.getDatasource().get(name);
       if(dataSourceItem != null){
           log.warn("数据源：{} 已经存在！",name);
           return;
       }
       dynamicDataSourceProperties.getDatasource().put(name,dataSourceProperty);

       reload();
   }

    private void addDataSource(){
        DataSourceProperty newMaster = new DataSourceProperty();
        newMaster.setUsername(DataBaseConfig.DATA_BASE_BOT_USERNAME);
        newMaster.setPassword(DataBaseConfig.DATA_BASE_BOT_PASSWORD);
        newMaster.setDriverClassName(DataBaseConfig.DATA_BASE_MASTER_DRIVERCLASSNAME);
        newMaster.setUrl(DataBaseConfig.JDBC_URL);
        newMaster.setDruid(new DruidConfig());

        // 将旧主数据拿出来 下面做替换
        DataSourceProperty oldMaster = dynamicDataSourceProperties.getDatasource().get(DataBaseConfig.DATA_SOURCE_MASTER);

        // 将程序配置的数据库作为主数据源
        dynamicDataSourceProperties.getDatasource().put(DataBaseConfig.DATA_SOURCE_MASTER,newMaster);
        dynamicDataSourceProperties.getDatasource().put(DataBaseConfig.DATA_SOURCE_SYSTEM,oldMaster);

        reload();
    }
    private void reload(){
        try {
            log.info("开始重新加载数据源...");
            dynamicRoutingDataSource.afterPropertiesSet();
        } catch (Exception e) {
            log.error("重新加载数据源失败",e);
            System.exit(0);
        }
    }

}
