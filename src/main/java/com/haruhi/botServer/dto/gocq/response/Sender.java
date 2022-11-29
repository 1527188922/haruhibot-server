package com.haruhi.botServer.dto.gocq.response;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

import java.io.Serializable;

@Data
public class Sender implements Serializable {
    private Integer age;
    private String area;
    private String card;
    private String level;
    private String role;
    private String nickname;
    private String sex;
    private String title;
    @JSONField(name = "user_id")
    private Long userId;

}
