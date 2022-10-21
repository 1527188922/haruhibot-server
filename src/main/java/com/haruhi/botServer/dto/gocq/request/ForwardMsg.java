package com.haruhi.botServer.dto.gocq.request;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@lombok.Data
@NoArgsConstructor
public class ForwardMsg {
    private String type = "node";
    private ForwardMsg.Data data;

    public ForwardMsg(ForwardMsg.Data data){
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

