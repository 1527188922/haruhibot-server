package com.haruhi.botserver.integration.onebot.model;

import lombok.Data;

@Data
public class DownloadFileResp {

    // gocq下载文件的绝对路径
    private String file;
}
