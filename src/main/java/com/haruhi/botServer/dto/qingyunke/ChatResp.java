package com.haruhi.botServer.dto.qingyunke;

import lombok.Data;

import java.io.Serializable;

@Data
public class ChatResp implements Serializable {

    private Integer result;
    private String content;
}
