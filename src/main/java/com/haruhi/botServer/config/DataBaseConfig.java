package com.haruhi.botServer.config;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.text.MessageFormat;

@Component
@Slf4j
@Getter
public class DataBaseConfig {

    public static final String JDBC_URL_TEMPLATE = "jdbc:mysql://{0}:{1}/{2}?useUnicode=true&characterEncoding=utf-8&useSSL=false";

    // bot数据源名称
    public final static String DATA_SOURCE_MASTER = "master";
    // 系统数据源名称
    public final static String DATA_SOURCE_SYSTEM = "system";

    // 群聊天历史
    public final static String T_GROUP_CHAT_HISTORY = "t_group_chat_history";
    // 话术
    public final static String T_VERBAL_TRICKS = "t_verbal_tricks";
    // 戳一戳回复表
    public final static String T_POKE_REPLY = "t_poke_reply";
    // 词条
    public final static String T_WORD_STRIP = "t_word_strip";
    // pixiv 图库表
    public final static String T_PIXIV = "t_pixiv";



    // 驱动类全命名 reference
    @Value("${spring.datasource.dynamic.datasource.master.driver-class-name}")
    private String masterDriverClassName;
    // bot数据库名称
    @Value("${mysql.dbName}")
    private String masterDBName;
    // 数据库用户名
    @Value("${mysql.username}")
    private String masterUsername;
    // 数据库密码
    @Value("${mysql.password}")
    private String masterPassword;
    //  数据库 host
    @Value("${mysql.host}")
    private String masterHost;
    // 数据库 port
    @Value("${mysql.port}")
    private String masterPort;
    private String masterJdbcUrl;


    @PostConstruct
    private void postConstruct(){
        masterJdbcUrl = MessageFormat.format(JDBC_URL_TEMPLATE, masterHost, masterPort, masterDBName);
        log.info("masterJdbcUrl:{}", masterJdbcUrl);
    }

}
