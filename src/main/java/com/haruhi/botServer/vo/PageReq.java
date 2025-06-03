package com.haruhi.botServer.vo;

import lombok.Data;

@Data
public class PageReq {

    private int currentPage;
    private int pageSize = 10;
}
