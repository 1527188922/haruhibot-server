package com.haruhi.botServer.config;


public class DataBaseConfig {

    public static final String JDBC_URL_TEMPLATE = "jdbc:mysql://{0}:{1}/{2}?useUnicode=true&characterEncoding=utf-8&useSSL=false";


    // bot数据源名称
    public final static String DATA_SOURCE_MASTER = "master";
    public final static String SQLITE_DATABASE_FILE_NAME = "haruhibot_server.db";
    public static final String SQLITE_DEFAULT_JDBC_URL = "jdbc:sqlite::resource:data/"+SQLITE_DATABASE_FILE_NAME;
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
    public final static String T_GROUP_INFO = "t_group_info";

}
