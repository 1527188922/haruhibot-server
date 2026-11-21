package com.haruhi.botserver.features.news.handler;

import com.haruhi.botserver.configuration.metadata.ConfigKey;
import com.haruhi.botserver.configuration.service.Configs;
import com.haruhi.botserver.shared.util.DateTimeUtil;
import org.springframework.stereotype.Component;

/**
 * 60秒读懂世界新闻图片，按当天日期拼接图片地址
 */
@Component
public class SixtySecondsNewsImageProvider implements NewsImageProvider {

    @Override
    public String name() {
        return "60s";
    }

    @Override
    public String imageUrl() {
        String date = DateTimeUtil.dateTimeFormat(System.currentTimeMillis(), DateTimeUtil.PatternEnum.yyyyMMdd);
        return Configs.getStr(ConfigKey.NEWS_60S_URL).replace("{date}", date);
    }
}
