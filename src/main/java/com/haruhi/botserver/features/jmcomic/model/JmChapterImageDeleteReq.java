package com.haruhi.botserver.features.jmcomic.model;

import lombok.Data;

import java.util.List;

@Data
public class JmChapterImageDeleteReq {
    private List<Long> ids;
    private Boolean deleteData;
    private Boolean deleteFile;
}
