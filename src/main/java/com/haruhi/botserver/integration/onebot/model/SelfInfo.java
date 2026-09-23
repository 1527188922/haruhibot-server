package com.haruhi.botserver.integration.onebot.model;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

import java.io.Serializable;

@Data
public class SelfInfo implements Serializable {
    @JSONField(name = "user_id")
    private Long userId;
    private String nickname;
}
