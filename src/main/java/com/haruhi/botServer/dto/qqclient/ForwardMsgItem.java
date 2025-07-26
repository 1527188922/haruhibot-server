package com.haruhi.botServer.dto.qqclient;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@lombok.Data
@NoArgsConstructor
public class ForwardMsgItem {
    private String type = "node";
    private ForwardMsgItem.Data data;

    public ForwardMsgItem(ForwardMsgItem.Data data){
        this.data = data;
    }

    @lombok.Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Data{
        @JSONField(name = "user_id")
        private Long userId;
        private String nickname;
        private List<MessageHolder> content;
    }


    public static ForwardMsgItem instance(Long uin,String name,List<MessageHolder> content){
        return new ForwardMsgItem(
                new Data(uin, name, content)
        );
    }

    public static ForwardMsgItem instance(Long uin,String name,MessageHolder content){
        return new ForwardMsgItem(
                new Data(uin, name, new ArrayList<>(Collections.singletonList(content)))
        );
    }
}

