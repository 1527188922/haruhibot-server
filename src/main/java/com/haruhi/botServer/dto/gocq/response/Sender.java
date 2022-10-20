package com.haruhi.botServer.dto.gocq.response;

import lombok.Data;

import java.io.Serializable;

@Data
public class Sender implements Serializable {
    private int age;
    private String area;
    private String card;
    private String level;
    private String role;
    private String nickname;
    private String sex;
    private String title;
    private Long user_id;

}
