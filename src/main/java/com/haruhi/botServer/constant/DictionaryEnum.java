package com.haruhi.botServer.constant;

import com.haruhi.botServer.service.JmcomicService;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum DictionaryEnum{

    BILIBILI_COOKIES_SESSDATA(true,"bilibili.cookies.sessdata",null,"b站cookie中获取，用于解析b站视频等需要调用b站api的功能"),
    BILIBILI_COOKIES_BILI_JCT(true,"bilibili.cookies.bili_jct",null,"b站cookie中获取，用于解析b站视频等需要调用b站api的功能"),
    BILIBILI_UPLOAD_VIDEO_DURATION_LIMIT(true,"bilibili.upload_video.duration_limit","600","上传b站视频时长限制，单位秒"),
    BILIBILI_DOWNLOAD_VIDEO_DURATION_LIMIT(true,"bilibili.download_video.duration_limit","600","下载b站视频时长限制，单位秒"),

    SAUCENAO_SEARCH_IMAGE__KEY(true,"saucenao.search_image_key",null,"用于请求识图接口认证,从https://saucenao.com获取"),

    QIANWEN_API_KEY(true,"qianwen.api_key",null,"请求阿里巴巴千问模型认证"),

    JM_PASSWORD_ZIP(true,"jm.password.zip", JmcomicService.JM_DEFAULT_PASSWORD,"jm本子zip包解压密码,注意：修改密码不会改变之前已经存在的zip包密码，重复下载可重新生成使用新密码的zip包"),
    JM_PASSWORD_PDF(true,"jm.password.pdf",JmcomicService.JM_DEFAULT_PASSWORD,"jm本子pdf保护密码,注意：修改密码不会改变之前已经存在的pdf文件密码，重复下载可重新生成使用新密码的pdf文件"),
    JM_DOWNLOAD_THREADS(true,"jm.download.threads",null,"下载本子时线程数量，未配置、0或小于0则等于CPU逻辑核心数量"),
    JM_ALBUM_NAME_MAX_LENGTH(true,"jm.album.name_max_length","215","jm本子名称最大长度(注意是名称的bytes.length而非名称字符个数！)，由于使用本子名做为下载后的本子文件名，文件名有长度限制，linux系统文件名称最长支持255，注意：这里配置要小于255，预留出文件名称后面拼接的jm号"),

    BOT_ACCESS_TOKEN(true,"bot.access_token",null,"机器人Websocket服务，建立连接握手时认证token，未配置表示无需认证"),
    BOT_SUPERUSERS(true,"bot.superusers","1527188922","机器人超级管理员qq号，多个qq号逗号分割,注意：未配置超级用户则一些超级用户功能不可使用"),
    BOT_MAX_CONNECTIONS(true,"bot.max_connections","5","机器人Websocket服务最大连接数，小于0表示无限制，0表示禁止连接（改成0不会断开已有连接）"),
    BOT_ACCESS_GROUP(true,"bot.access_groups",null,"可使用机器人的群号，多个时逗号分开，若不配置表示所有群都可使用机器人"),

    SWITCH_DISABLE_GROUP(true,"switch.disable_group","false","是否禁用所有群功能（对有所有群消息不予理睬，但会保留保存聊天记录功能），true:禁用"),
    SWITCH_QINGYUNKE_CHAT(true,"switch.qingyunke_chat","true","是否启用青云可聊天api，当at机器人或私聊机器人任何命令都未触发时，会触发该api，true:启用"),
    SWITCH_SEARCH_IMAGE_ALLOW_GROUP(true,"switch.search_image_allow_group","true","是否允许群聊中使用识图功能，true:允许"),
    SWITCH_SEARCH_BT_ALLOW_GROUP(true,"switch.search_bt_allow_group","true","是否允许群聊中使用bt搜索功能，true:允许"),
    SWITCH_GROUP_INCREASE(true,"switch.group_increase","true","是否开启加群提示，true:开启"),
    SWITCH_GROUP_DECREASE(true,"switch.group_decrease","true","是否开启群成员离群提示，true:开启"),

    URL_CONF_AGEFANS(true,"url_conf.agefans","https://www.agemys.vip","agefans网站地址，用于今日新番功能，末尾不需斜杠，备用：https://www.age.tv，https://www.agemys.net，https://www.agemys.cc"),
    URL_CONF_BT_SEARCH(true,"url_conf.bt_search","http://www.eclzz.bio","磁力搜索网站地址，用于bt搜索功能，末尾不需斜杠，备用：http://www.eclzz.art\n" +
            "http://www.eclzz.win\n" +
            "http://www.eclzz.love\n" +
            "http://www.eclzz.guru\n" +
            "http://www.eclzz.city\n" +
            "http://www.eclzz.me\n" +
            "http://www.eclzz.ink\n" +
            "http://www.eclzz.mobi\n" +
            "http://www.eclzz.wiki\n" +
            "http://www.eclzz.bio\n" +
            "http://www.eclzz.lat"),

    DATABASE_DB_SQL_CACHE(false,"db.sql_cache","","WebUI sql编辑器内容缓存"),

    ;
    private final boolean needInit;
    private final String key;
    private final String defaultValue;
    private final String remark;
}