package com.haruhi.botServer.vo;

import lombok.Data;

import java.util.List;

@Data
public class JmAlbumDeleteReq {
    private List<Long> ids;
    private Boolean deletePdf;
    private Boolean deleteZip;
    private Boolean deleteImages;
}
