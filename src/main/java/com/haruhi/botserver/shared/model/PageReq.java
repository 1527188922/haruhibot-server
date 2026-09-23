package com.haruhi.botserver.shared.model;

import lombok.Data;

@Data
public class PageReq {

    private int currentPage = 1;
    private int pageSize = 10;
}
