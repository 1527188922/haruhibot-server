package com.haruhi.botServer.dto.gocq.response;

import lombok.Data;

import java.io.Serializable;

@Data
public class SelfInfo implements Serializable {
    private String user_id;
    private String nickname;
}
