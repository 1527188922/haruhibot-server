package com.haruhi.botServer.dto.gocq.request;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

import java.util.Collection;

@Data
public class Params<T> {
    @JSONField(name = "message_type")
    private String messageType;
    @JSONField(name = "user_id")
    private Long userId;
    @JSONField(name = "group_id")
    private Long groupId;
    private T message;
    private Collection messages;
    @JSONField(name = "auto_escape")
    private boolean autoEscape;
}
