package com.haruhi.botserver.features.ai.client.model.qingyunke;

import lombok.Data;

import java.io.Serializable;

@Data
public class ChatResp implements Serializable {

    private Integer result;
    private String content;
}
