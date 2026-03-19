package com.haruhi.botServer.vo;

import lombok.Data;

@Data
public class PageReq {

    private int currentPage = 1;
    private int pageSize = 10;
}
