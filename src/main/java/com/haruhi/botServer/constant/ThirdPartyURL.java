package com.haruhi.botServer.constant;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.io.FileUtil;
import com.haruhi.botServer.utils.Base64Util;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

public class ThirdPartyURL {

    // 识图
    public static String SEARCH_IMAGE = "https://saucenao.com/search.php";
    // 青云客 ai聊天
    public static final String QINGYUNKE_AI_CHAT = "http://api.qingyunke.com/api.php";
    // b站 获取弹幕 https://comment.bilibili.com/291456105.xml cid=291456105
    // https://api.bilibili.com/x/v2/dm/web/history/seg.so?oid=291456105&type=1?date=2025-09-09
    public static String BULLET_CHAR = "https://api.bilibili.com/x/v1/dm/list.so";
    // b站链接
    public static String BILIBILI_URL = "https://www.bilibili.com/video";
    // b站 获取cid
    public static String PLAYER_CID = "https://api.bilibili.com/x/player/pagelist";
    // lolicon
    public static String LOLICON = "https://api.lolicon.app/setu/v2";
    // 预览磁力链接
    public static String WHATS_LINK = "https://whatslink.info/api/v1/link";

    // 网易新闻
    public static String NEWS_163 = "http://c.m.163.com/nc/article/headline/T1348647853363/0-40.html";
    // 网易搜索歌曲
    public static String NETEASE_SEARCH_MUSIC = "http://music.163.com/weapi/cloudsearch/get/web";

    public static void main(String[] args) {
        byte[] decode = FileUtil.readBytes(new File("C:\\Users\\15271\\Desktop\\temp\\seg.so"));

        DanmakuOuterClass.Danmaku danmaku = DanmakuOuterClass.Danmaku.parseFrom(data);
    }

}
