package com.haruhi.botServer.dto.gocq.request;

import lombok.Data;

import java.util.Collection;

@Data
public class Params {
    private String message_type;
    private Long user_id;
    private Long group_id;
    private String message;
    private Collection messages;
    private boolean auto_escape;
}
