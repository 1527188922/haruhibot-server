package com.haruhi.botServer.config;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@Getter
public class DataBaseConfig {

    public static final String JDBC_URL_TEMPLATE = "jdbc:mysql://{0}:{1}/{2}?useUnicode=true&characterEncoding=utf-8&useSSL=false";

    // bot数据源名称
    public final static String DATA_SOURCE_MASTER = "master";
    // mysql系统数据源名称
    public final static String DATA_SOURCE_MYSQL_SYSTEM = "mysql_system";
    public final static String DATA_SOURCE_MYSQL = "mysql";

    // 群聊天历史
    public final static String T_CHAT_RECORD = "t_chat_record";
    // 话术
    public final static String T_CUSTOM_REPLY = "t_custom_reply";
    // 戳一戳回复表
    public final static String T_POKE_REPLY = "t_poke_reply";
    // 词条
    public final static String T_WORD_STRIP = "t_word_strip";
    // pixiv 图库表
    public final static String T_PIXIV = "t_pixiv";
    public final static String T_SEND_LIKE_RECORD = "t_send_like_record";
    public final static String T_DICTIONARY = "t_dictionary";



    // 驱动类全命名 reference
//    @Value("${spring.datasource.dynamic.datasource.master.driver-class-name}")
    private String masterDriverClassName;
    // bot数据库名称
//    @Value("${mysql.dbName}")
    private String masterDBName;
    // 数据库用户名
//    @Value("${mysql.username}")
    private String masterUsername;
    // 数据库密码
//    @Value("${mysql.password}")
    private String masterPassword;
    //  数据库 host
//    @Value("${mysql.host}")
    private String masterHost;
    // 数据库 port
//    @Value("${mysql.port}")
    private String masterPort;
    private String masterJdbcUrl;


//    @PostConstruct
//    private void postConstruct(){
//        File file = new File(FileUtil.getAppDir() + File.separator + "data\\haruhibot_server.db");
//
//        FileUtil.mkdirs(file.getParent());
//        if (!file.exists()) {
//            try {
//                file.createNewFile();
//            } catch (IOException e) {
//                throw new RuntimeException(e);
//            }
//        }
//
//
//        masterJdbcUrl = MessageFormat.format(JDBC_URL_TEMPLATE, masterHost, masterPort, masterDBName);
//        log.info("masterJdbcUrl:{}", masterJdbcUrl);
//    }

}
