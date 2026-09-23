package com.haruhi.botserver.administration.model;

import lombok.Data;

@Data
public class BotWebSocketInfo{
        private Boolean running;
        private Integer connections;
        private Integer maxConnections;
        private String path;
        private String accessToken;
}