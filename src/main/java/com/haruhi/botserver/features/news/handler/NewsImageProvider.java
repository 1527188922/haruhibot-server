package com.haruhi.botserver.features.news.handler;

/**
 * 新闻图片提供者
 */
public interface NewsImageProvider {


    default int sort(){
        return 0;
    };

    /**
     * 提供者名称，用于日志
     */
    String name();

    /**
     * 返回新闻图片地址，获取失败时抛出异常
     */
    String imageUrl();
}
