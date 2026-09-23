package com.haruhi.botserver.integration.onebot.model;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

@Data
public class SendMsgResp {

    @JSONField(name = "message_id")
    private Long messageId;
}
