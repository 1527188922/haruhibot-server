package com.haruhi.botServer.dto.qqclient;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ForwardMsgParams {

    @JSONField(name = "group_id")
    private Long groupId;
    @JSONField(name = "user_id")
    private Long userId;
    @JSONField(name = "message_type")
    private String messageType;
    private List<ForwardMsgItem> messages;
    private List<News> news;
    private String prompt;
    private String summary;
    private String source;

    @lombok.Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class News{
        private String text;
    }

}
