package com.haruhi.botServer.dto.qqclient;

import lombok.Data;

@Data
public class DownloadFileResp {

    // gocq下载文件的绝对路径
    private String file;
}
