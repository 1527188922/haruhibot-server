package com.haruhi.botServer.constant;

public enum RegexEnum {
    CQ_CODE(".*\\[CQ:.*\\].*"),
    CQ_CODE_REPLACR("\\[CQ:.*\\]"),
    CHECKIN("签到|打卡"),
    SEE_FAVORABILITY("好感度|我的好感|我的好感度|查看好感|查看好感度|查看我的好感度"),
    COLLECTION("添加收藏.*|新增收藏.*|增加收藏.*"),
    COLLECTION_SPLIT("添加收藏|新增收藏|增加收藏"),
    COLLECTION_CANCEL("取消|算了|取消收藏"),
    SEARCH_IMAGE("识图|搜图"),
    PIXIV("pix|PIX"),
    PIXIV_R("pixr|PIXR"),
    PIXIV_PID("ppid|PPID"),
    PIXIV_UID("puid|PUID"),
    PIXIV_COUNT("pix统计|PIX统计"),
    FRIEND_SAID("朋友说|我朋友说|我朋友都说|朋友都说|朋友说过|我朋友说过|我朋友老说|我朋友总说|我朋友老是说|我朋友总是说|我朋友他说|我朋友她说|我朋友它说|朋友他说|朋友她说|朋友它说"),
    WORD_STRIP_ADD("添加词条(.*?)答"),
    WORD_STRIP_DELETE("删除词条"),
    WORD_STRIP_SHOW("本群词条|所有词条|查看所有词条|显示所有词条"),
    BULLET_CHAT_WORD_CLOUD("弹幕词云"),
    SCOLD_ME_DG("骂我|钉宫|钉宫理惠"),
    BT_SEARCH("bt"),
    BT_SEARCH_HAS_PAGE("bt(.*?)页"),
    GROUP_BROADCAST_MESSAGES("群广播：|群广播:"),
    NEW_ANIMATION_TODAY("今日新番"),
    SHOW_ALL_FUNCTION("所有功能|显示所有功能"),
    DISABLE_FUNCTION("禁用功能|关闭功能"),
    ENABLE_FUNCTION("开启功能|启用功能"),
    GROUP_DISABLE_FUNCTION("群禁用功能|群关闭功能"),
    GROUP_ENABLE_FUNCTION("群开启功能|群启用功能"),
    SEE_TODAY_NEWS("今日新闻"),
    SUBSCRIBE_NEWS("订阅新闻"),
    UN_SUBSCRIBE_NEWS("取消订阅新闻"),
    GAME_RUSSIAN_ROULETTE("发起俄罗斯轮盘"),
    MUSIC_CARD("点歌 "),
    FLUSH_CACHE("#刷新缓存"),
    CHATGPT("gpt "),
    START_MONITOR_LOG("/startml|/开启监听日志"),
    STOP_MONITOR_LOG("/stopml|/关闭监听日志"),
    SEND_LOG("/sendlog|/发送日志"),
    STATUS("/status|/状态");


    private final String value;
    RegexEnum(String value){
        this.value = value;
    }
    public String getValue(){
        return value;
    }
}
