//package com.haruhi.botServer.service;
//
//import com.baomidou.dynamic.datasource.DynamicDataSourceCreator;
//import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
//import com.baomidou.dynamic.datasource.spring.boot.autoconfigure.DataSourceProperty;
//import com.haruhi.botServer.config.DataBaseConfig;
//import com.haruhi.botServer.mapper.system.TableInitMapper;
//import com.haruhi.botServer.mapper.system.DataBaseInitMapper;
//import com.zaxxer.hikari.HikariDataSource;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//
//import javax.annotation.PostConstruct;
//import javax.management.openmbean.KeyAlreadyExistsException;
//import javax.sql.DataSource;
//
//@Slf4j
//@Service
//public class DataBaseService {
//
//    private final DataBaseInitMapper dataBaseInitMapper;
//    private final TableInitMapper tableInitMapper;
//    private final DataBaseConfig dataBaseConfig;
//    private final DynamicDataSourceCreator dynamicDataSourceCreator;
//    private final DataSource dataSource;
//
//    public DataBaseService(DataBaseInitMapper dataBaseInitMapper, TableInitMapper tableInitMapper, DataBaseConfig dataBaseConfig, DynamicDataSourceCreator dynamicDataSourceCreator, DataSource dataSource) {
//        this.dataBaseInitMapper = dataBaseInitMapper;
//        this.tableInitMapper = tableInitMapper;
//        this.dataBaseConfig = dataBaseConfig;
//        this.dynamicDataSourceCreator = dynamicDataSourceCreator;
//        this.dataSource = dataSource;
//    }
//
//    private DynamicRoutingDataSource getDynamicRoutingDataSource(){
//        return (DynamicRoutingDataSource)dataSource;
//    }
//
//    @PostConstruct
//    private void init(){
//        log.info("开始初始化数据库...");
//        try {
//            if(dataBaseInitMapper.dataBaseIsExist(dataBaseConfig.getMasterDBName()) == 0){
//                log.info("数据库不存在,开始创建:{}",dataBaseConfig.getMasterDBName());
//                dataBaseInitMapper.createDataBase(dataBaseConfig.getMasterDBName());
//                log.info("数据库创建成功");
//            }
//
//            addDataSource();
//
//            if (dataBaseInitMapper.tableIsExist(dataBaseConfig.getMasterDBName(),DataBaseConfig.T_CHAT_RECORD) == 0) {
//                tableInitMapper.createChatRecord(DataBaseConfig.T_CHAT_RECORD);
//            }
//
//            if (dataBaseInitMapper.tableIsExist(dataBaseConfig.getMasterDBName(),DataBaseConfig.T_CUSTOM_REPLY) == 0) {
//                tableInitMapper.createCustomReply(DataBaseConfig.T_CUSTOM_REPLY);
//            }
//            if (dataBaseInitMapper.tableIsExist(dataBaseConfig.getMasterDBName(),DataBaseConfig.T_POKE_REPLY) == 0) {
//                tableInitMapper.createPokeReply(DataBaseConfig.T_POKE_REPLY);
//            }
//            if(dataBaseInitMapper.tableIsExist(dataBaseConfig.getMasterDBName(),DataBaseConfig.T_WORD_STRIP) == 0){
//                tableInitMapper.createWordStrip(DataBaseConfig.T_WORD_STRIP);
//            }
//            if(dataBaseInitMapper.tableIsExist(dataBaseConfig.getMasterDBName(),DataBaseConfig.T_PIXIV) == 0){
//                tableInitMapper.createPixiv(DataBaseConfig.T_PIXIV);
//            }
//            if(dataBaseInitMapper.tableIsExist(dataBaseConfig.getMasterDBName(),DataBaseConfig.T_SEND_LIKE_RECORD) == 0){
//                tableInitMapper.createSendLikeRecord(DataBaseConfig.T_SEND_LIKE_RECORD);
//            }
//            if(dataBaseInitMapper.tableIsExist(dataBaseConfig.getMasterDBName(),DataBaseConfig.T_DICTIONARY) == 0){
//                tableInitMapper.createDictionary(DataBaseConfig.T_DICTIONARY);
//            }
//
//            log.info("初始化数据库完成");
//        }catch (Exception e){
//            log.error("初始化数据库异常",e);
//            System.exit(0);
//        }
//    }
//
//    /**
//     * 添加bot数据库的数据源
//     * 并置换主次数据源 让bot数据源为master
//     */
//    private void addDataSource(){
//        // 取出旧的master数据源
//        DataSource oldMaster = getDataSource(DataBaseConfig.DATA_SOURCE_MASTER);
//
//        // 创建新的数据源
//        DataSourceProperty newMasterProperty = new DataSourceProperty();
//        newMasterProperty.setUsername(dataBaseConfig.getMasterUsername());
//        newMasterProperty.setPassword(dataBaseConfig.getMasterPassword());
//        newMasterProperty.setDriverClassName(dataBaseConfig.getMasterDriverClassName());
//        newMasterProperty.setUrl(dataBaseConfig.getMasterJdbcUrl());
//        newMasterProperty.setType(HikariDataSource.class);
//        DataSource newDataSource = dynamicDataSourceCreator.createDataSource(newMasterProperty);
//        // 连接池名称一旦设置不可更改 在置换数据源之后 名称为master的连接池的数据源实际上是system
//        // 而新的数据源连接池名称是HikariPool-number
//        // 可以自定义为新的数据源连接池设置一个唯一名称
////        newMasterProperty.setPollName("newMaster");
//
//        // 覆盖map中master数据源
//        addDataSource(DataBaseConfig.DATA_SOURCE_MASTER,newDataSource);
//        // 将旧的master数据源重新put进map key为system
//        addDataSource(DataBaseConfig.DATA_SOURCE_SYSTEM,oldMaster);
//
//        /***************************************   置换主次数据源方式二   ***************************************/
//
////        // 将旧主数据拿出来 下面做替换
////        DataSourceProperty oldMaster = dynamicDataSourceProperties.getDatasource().get(DataBaseConfig.DATA_SOURCE_MASTER);
////
////        // 将程序配置的数据库作为主数据源
////        dynamicDataSourceProperties.getDatasource().put(DataBaseConfig.DATA_SOURCE_MASTER,newMaster);
////        dynamicDataSourceProperties.getDatasource().put(DataBaseConfig.DATA_SOURCE_SYSTEM,oldMaster);
//
//        /**
//         * afterPropertiesSet()方法最终是从DynamicDataSourceProperties中加载数据源
//         * 而DynamicDataSourceProperties数据来源是yml配置文件
//         * 改变DynamicDataSourceProperties中的属性：Map<String, DataSourceProperty> datasource中的内容，调用afterPropertiesSet()方法才能生效
//         */
////        try {
////            log.info("开始重新加载数据源...");
////            dynamicRoutingDataSource.afterPropertiesSet();
////        } catch (Exception e) {
////            log.error("重新加载数据源失败",e);
////            System.exit(0);
////        }
//
//    }
//
//    private void addDataSource(String ds,DataSource dataSource){
//        getDynamicRoutingDataSource().addDataSource(ds,dataSource);
//    }
//
//    /**
//     * 创建并且添加数据源
//     * @param ds 数据源名称
//     * @param newDataSourceProperty 数据源属性
//     * @return 新的数据源
//     */
//    public DataSource addDataSource(String ds,DataSourceProperty newDataSourceProperty){
//        if (dataSourceIsExist(ds)) {
//            throw new KeyAlreadyExistsException(String.format("数据源名称已经被使用：%s",ds));
//        }
//        DataSource newDataSource = dynamicDataSourceCreator.createDataSource(newDataSourceProperty);
//        addDataSource(ds,newDataSource);
//        return newDataSource;
//    }
//
//    /**
//     * 数据源名称是否已存在
//     * @param ds 数据源名称
//     * @return true:已存在
//     */
//    public boolean dataSourceIsExist(String ds){
//        return getDynamicRoutingDataSource().getCurrentDataSources().containsKey(ds);
//    }
//
//    /**
//     * 从DynamicRoutingDataSource中获取具体的数据源（hikari或druid）
//     * 不存在返回null
//     * @param ds 数据源名称
//     * @return
//     */
//    public DataSource getDataSource(String ds){
//        if (!dataSourceIsExist(ds)) {
//            return null;
//        }
//        // getDataSource(ds)：如果ds不存在 返回primary数据源（primary是一个变量，具体取出哪个数据源得看primary值是什么）
//        return getDynamicRoutingDataSource().getDataSource(ds);
//    }
//
//}
