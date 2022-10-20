package com.haruhi.botServer.dto.gocq.request;

@lombok.Data
public class ForwardMsg {
    private String type = "node";
    private ForwardMsg.Data data;

    @lombok.Data
    public static class Data{
        private String name;
        private Long uin;
        private String content;
    }
}

