package com.haruhi.botServer.dto.qqclient;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

@Data
public class SendMsgResp {

    @JSONField(name = "message_id")
    private Long messageId;
}
