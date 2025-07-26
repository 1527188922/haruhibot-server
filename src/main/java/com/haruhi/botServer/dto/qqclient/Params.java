package com.haruhi.botServer.dto.qqclient;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;


@Data
public class Params<T> {
    @JSONField(name = "message_type")
    private String messageType;
    @JSONField(name = "user_id")
    private Long userId;
    @JSONField(name = "group_id")
    private Long groupId;
    private T message;

    @JSONField(name = "auto_escape")
    private boolean autoEscape;
}
