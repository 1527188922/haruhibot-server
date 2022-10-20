package com.haruhi.botServer.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class BotConfig {

    public static String NAME = "";
    public static String SEARCH_IMAGE_KEY = "";
    public static String ACCESS_TOKEN = "";

    @Autowired
    public void setName(@Value("${bot.name}") String name) {
        NAME = Strings.isBlank(name) ? "春日酱" : name;
    }

    @Autowired
    public void setAccessToken(@Value("${bot.access-token}") String accessToken){
        ACCESS_TOKEN = accessToken;
        if(Strings.isBlank(ACCESS_TOKEN)){
            log.warn("未配置access-token！");
        }
    }

    @Autowired
    public void setSearchImageKey(@Value("${bot.search-image-key}") String searchImageKey){
        SEARCH_IMAGE_KEY = searchImageKey;
        if(Strings.isBlank(SEARCH_IMAGE_KEY)){
            log.warn("未配置识图key,无法使用识图功能");
        }
    }





}
