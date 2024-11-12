package com.haruhi.botServer.dto.napcat;

import com.alibaba.fastjson.annotation.JSONField;
import com.haruhi.botServer.constant.event.MessageTypeEnum;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Data
@Builder
public class ForwardMsg {

    @JSONField(name = "group_id")
    private Long groupId;
    @JSONField(name = "user_id")
    private Long userId;
    private List<Message> messages;
    private List<News> news;
    private String prompt;
    private String summary;
    private String source;

    @lombok.Data
    @Builder
    public static class Message{
        private String type = "node";
        private Data data;
    }
    @lombok.Data
    @Builder
    public static class Data{
        @JSONField(name = "user_id")
        private Long userId;
        private String nickname;
        private List<Content> content;
    }
    @lombok.Data
    @Builder
    public static class Content{
        private String type = "text";
        private ContentData data;
    }
    @lombok.Data
    @Builder
    public static class ContentData{
        private String text;
    }
    @lombok.Data
    @Builder
    public static class News{
        private String text;
    }


    public static ForwardMsg instance(String messageType, Long to, Long uin, String name,List<String> context){
        ForwardMsgBuilder forwardMsgBuilder = ForwardMsg.builder();
        if(MessageTypeEnum.group.getType().equals(messageType)){
            forwardMsgBuilder.groupId(to);
        }else if(MessageTypeEnum.privat.getType().equals(messageType)){
            forwardMsgBuilder.userId(to);
        }

        ArrayList<Message> messages = new ArrayList<>();
        for (String s : context) {
            Message message = Message.builder()
                    .data(Data.builder()
                            .userId(uin)
                            .nickname(name)
                            .content(
                                    Collections.singletonList(
                                            Content.builder()
                                                    .data(
                                                            ContentData.builder()
                                                                    .text(s)
                                                                    .build()
                                                    ).build()))
                            .build())
                    .build();
            messages.add(message);
        }
        return forwardMsgBuilder.messages(messages)
                .prompt("prompt")
                .summary("summary")
                .source("source")
                .news(Collections.singletonList(News.builder().text("news").build()))
                .build();
    }
}
