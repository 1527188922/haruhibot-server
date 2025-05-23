package com.haruhi.botServer.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SwitchConfig {
    
    public static boolean ENABLE_AI_CHAT;
    public static boolean SEARCH_IMAGE_ALLOW_GROUP;
    public static boolean GROUP_INCREASE;
    public static boolean GROUP_DECREASE;
    public static boolean SEARCH_BT_ALLOW_GROUP;
    public static boolean DISABLE_GROUP;//true:禁用所有群功能
    
    public SwitchConfig(@Value("${switch.ai-chat}") String aiChat,
                        @Value("${switch.search-image-allow-group}") String searchImageAllowGroup,
                        @Value("${switch.group-increase}") String groupIncrease,
                        @Value("${switch.group-decrease}") String groupDecrease,
                        @Value("${switch.search-bt-allow-group}") String searchBtAllowGroup,
                        @Value("${switch.disableGroup}") String disableGroup){

        ENABLE_AI_CHAT = "1".equals(aiChat);
        SEARCH_IMAGE_ALLOW_GROUP = "1".equals(searchImageAllowGroup);
        GROUP_INCREASE = "1".equals(groupIncrease);
        GROUP_DECREASE = "1".equals(groupDecrease);
        SEARCH_BT_ALLOW_GROUP = "1".equals(searchBtAllowGroup);
        DISABLE_GROUP = "1".equals(disableGroup);
    }
}
