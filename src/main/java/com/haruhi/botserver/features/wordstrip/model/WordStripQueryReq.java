package com.haruhi.botserver.features.wordstrip.model;

import com.haruhi.botserver.shared.model.PageReq;

import lombok.Data;

@Data
public class WordStripQueryReq extends PageReq {

    private Long userId;
    private Long groupId;
    private Long selfId;
    private String keyWord;
    private String answer;
}
