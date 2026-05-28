package com.haruhi.botServer.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CodeNameResp {
    private Serializable code;
    private String name;

    private Long userId;
    private String card;

    private String time;

    private String userAvatarUrl;
}
