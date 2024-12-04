package com.haruhi.botServer.constant;

import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ThirdPartyURL {

    // 识图
    public static String SEARCH_IMAGE = "https://saucenao.com/search.php";
    // 青云客 ai聊天
    public static String AI_CHAT = "http://api.qingyunke.com/api.php";
    // b站 获取弹幕
    public static String BULLET_CHAR = "https://api.bilibili.com/x/v1/dm/list.so";
    // b站链接
    public static String BILIBILI_URL = "https://www.bilibili.com/video";
    // b站 获取cid
    public static String PLAYER_CID = "https://api.bilibili.com/x/player/pagelist";
    // lolicon
    public static String LOLICON = "https://api.lolicon.app/setu/v2";
    // 预览磁力链接
    public static String WHATS_LINK = "https://whatslink.info/api/v1/link";

    /**
     * bt搜索备用域名
     * http://www.eclzz.art
     * http://www.eclzz.win
     * http://www.eclzz.love
     * http://www.eclzz.guru
     * http://www.eclzz.city
     * http://www.eclzz.me
     * http://www.eclzz.ink
     * http://www.eclzz.mobi
     * http://www.eclzz.wiki
     * http://www.eclzz.bio
     * http://www.eclzz.lat
     */
    public static String BT_SEARCH = "http://www.eclzz.me";
    // agefans.tv 今日新番使用 备用：www.age.tv
//    public static final String AGEFANSTV = "https://www.agemys.cc";
    public static String AGEFANSTV = "https://www.agemys.net";
    // 网易新闻
    public static String NEWS_163 = "http://c.m.163.com/nc/article/headline/T1348647853363/0-40.html";
    // 网易搜索歌曲
    public static String NETEASE_SEARCH_MUSIC = "http://music.163.com/weapi/cloudsearch/get/web";
    
    
    public ThirdPartyURL(@Value("${url-conf.bt-search}") String btSearch,@Value("${url-conf.agefans}") String agefans){
        if(Strings.isNotBlank(btSearch)){
            BT_SEARCH = btSearch;
        }
        if(Strings.isNotBlank(agefans)){
            AGEFANSTV = agefans;
        }
    }
   
}
