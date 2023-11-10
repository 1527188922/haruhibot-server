package com.haruhi.botServer.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SwitchConfig {
    
    public static boolean ENABLE_AI_CHAT;
    public static boolean SEARCH_IMAGE_ALLOW_GROUP;
    public static boolean GROUP_INCREASE;
    public static boolean GROUP_DECREASE;
    
    public SwitchConfig(@Value("${switch.ai-chat}") String aiChat,
                        @Value("${switch.search-image-allow-group}") String searchImageAllowGroup,
                        @Value("${switch.group-increase}") String groupIncrease,
                        @Value("${switch.group-decrease}") String groupDecrease){
        ENABLE_AI_CHAT = "1".equals(aiChat);
        SEARCH_IMAGE_ALLOW_GROUP = "1".equals(searchImageAllowGroup);
        GROUP_INCREASE = "1".equals(groupIncrease);
        GROUP_DECREASE = "1".equals(groupDecrease);
    }
}
