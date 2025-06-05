package com.haruhi.botServer.vo;

import lombok.Data;

@Data
public class FileNode {

    private String fileName;
    private boolean leaf;
    private String absolutePath;
    private boolean isDirectory;
    private Long size;
}
