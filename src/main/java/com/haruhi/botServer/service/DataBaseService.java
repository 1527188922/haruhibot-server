package com.haruhi.botServer.service;

import com.baomidou.dynamic.datasource.DynamicDataSourceCreator;
import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import com.baomidou.dynamic.datasource.spring.boot.autoconfigure.DataSourceProperty;
import com.baomidou.dynamic.datasource.spring.boot.autoconfigure.DynamicDataSourceProperties;
import com.haruhi.botServer.config.DataBaseConfig;
import com.haruhi.botServer.mapper.TableInitMapper;
import com.haruhi.botServer.mapper.system.DataBaseInitMapper;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Slf4j
@Service
public class DataBaseService {

    @Autowired
    private DataBaseInitMapper dataBaseInitMapper;
    @Autowired
    private TableInitMapper tableInitMapper;
//    @Autowired
//    private DynamicDataSourceProperties dynamicDataSourceProperties;
    @Autowired
    private PlatformTransactionManager platformTransactionManager;
    @Autowired
    private DynamicDataSourceCreator dynamicDataSourceCreator;


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

            if (dataBaseInitMapper.tableIsExist(DataBaseConfig.DATA_BASE_BOT,DataBaseConfig.T_VERBAL_TRICKS) == 0) {
                tableInitMapper.createVerbalTricks(DataBaseConfig.T_VERBAL_TRICKS);
            }
            if (dataBaseInitMapper.tableIsExist(DataBaseConfig.DATA_BASE_BOT,DataBaseConfig.T_POKE_REPLY) == 0) {
                tableInitMapper.createPokeReply(DataBaseConfig.T_POKE_REPLY);
            }
            if(dataBaseInitMapper.tableIsExist(DataBaseConfig.DATA_BASE_BOT,DataBaseConfig.T_WORD_STRIP) == 0){
                tableInitMapper.createWordStrip(DataBaseConfig.T_WORD_STRIP);
            }
            if(dataBaseInitMapper.tableIsExist(DataBaseConfig.DATA_BASE_BOT,DataBaseConfig.T_PIXIV) == 0){
                tableInitMapper.createPixiv(DataBaseConfig.T_PIXIV);
            }

            log.info("初始化数据库完成");
        }catch (Exception e){
            log.error("初始化数据库异常",e);
            System.exit(0);
        }
    }

    public void addDataSource(String dataSourceName,DataSource dataSource){
        DynamicRoutingDataSource dynamicRoutingDataSource = getDynamicRoutingDataSource();
        dynamicRoutingDataSource.addDataSource(dataSourceName,dataSource);
    }

    public void addDataSource(String dataSourceName,DataSourceProperty newDataSourceProperty){
        DataSource newDataSource = dynamicDataSourceCreator.createDataSource(newDataSourceProperty);
        addDataSource(dataSourceName,newDataSource);

    }


    /**
     * 添加bot数据库的数据源
     * 并置换主次数据源 让bot数据源为master
     */
    private void addDataSource(){
        // 取出旧的master数据源
        DynamicRoutingDataSource dynamicRoutingDataSource = getDynamicRoutingDataSource();
        HikariDataSource oldMaster = (HikariDataSource)dynamicRoutingDataSource.getDataSource(DataBaseConfig.DATA_SOURCE_MASTER);

        DataSourceProperty newMasterProperty = new DataSourceProperty();
        newMasterProperty.setUsername(DataBaseConfig.DATA_BASE_BOT_USERNAME);
        newMasterProperty.setPassword(DataBaseConfig.DATA_BASE_BOT_PASSWORD);
        newMasterProperty.setDriverClassName(DataBaseConfig.DATA_BASE_MASTER_DRIVERCLASSNAME);
        newMasterProperty.setUrl(DataBaseConfig.JDBC_URL);
        newMasterProperty.setType(HikariDataSource.class);
        // 连接池名称一旦设置不可更改 在置换数据源之后 名称为master的连接池的数据源实际上是system
        // 而新的数据源连接池名称是HikariPool-number
        // 可以自定义为新的数据源连接池设置一个唯一名称
//        newMasterProperty.setPollName("newMaster");

        // 覆盖map中master数据源
        addDataSource(DataBaseConfig.DATA_SOURCE_MASTER,newMasterProperty);
        // 将旧的master数据源重新put进map key为system
        addDataSource(DataBaseConfig.DATA_SOURCE_SYSTEM,oldMaster);

        /***************************************   置换主次数据源方式二   ***************************************/

//        // 将旧主数据拿出来 下面做替换
//        DataSourceProperty oldMaster = dynamicDataSourceProperties.getDatasource().get(DataBaseConfig.DATA_SOURCE_MASTER);
//
//        // 将程序配置的数据库作为主数据源
//        dynamicDataSourceProperties.getDatasource().put(DataBaseConfig.DATA_SOURCE_MASTER,newMaster);
//        dynamicDataSourceProperties.getDatasource().put(DataBaseConfig.DATA_SOURCE_SYSTEM,oldMaster);

        /**
         * afterPropertiesSet()方法最终是从DynamicDataSourceProperties中加载数据源
         * 而DynamicDataSourceProperties数据来源是yml配置文件
         * 改变DynamicDataSourceProperties中的属性：Map<String, DataSourceProperty> datasource中的内容，调用afterPropertiesSet()方法才能生效
         */
//        try {
//            log.info("开始重新加载数据源...");
//            dynamicRoutingDataSource.afterPropertiesSet();
//        } catch (Exception e) {
//            log.error("重新加载数据源失败",e);
//            System.exit(0);
//        }

    }


    public DynamicRoutingDataSource getDynamicRoutingDataSource(){
        JdbcTransactionManager jdbcTransactionManager = (JdbcTransactionManager) platformTransactionManager;
        return (DynamicRoutingDataSource)jdbcTransactionManager.getDataSource();
    }

}
