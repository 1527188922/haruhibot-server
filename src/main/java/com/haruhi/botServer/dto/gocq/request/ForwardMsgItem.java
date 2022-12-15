package com.haruhi.botServer.dto.gocq.request;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

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
        private String name;
        private Long uin;
        private String content;
    }
}

