package com.haruhi.botserver.features.jmcomic.client.model.jmcomic;

import lombok.Data;

import java.io.File;

@Data
public class DownloadParam {
    private String imgUrl;
    private String filename;
    private File imgFile;
    private int blockNum;
}
