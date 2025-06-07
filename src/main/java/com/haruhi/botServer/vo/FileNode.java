package com.haruhi.botServer.vo;

import lombok.Data;

@Data
public class FileNode {

    private String fileName;
    private Boolean leaf;
    private String absolutePath;
    private Boolean isDirectory;
    private Long size;
    private Boolean showPreview;
    private Boolean showDel;
}
