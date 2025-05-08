package com.haruhi.botServer.dto.jmcomic;

import lombok.Data;

@Data
public class DownloadParam {
    private String imgUrl;
    private String filename;
    private String imgFilePath;
}
