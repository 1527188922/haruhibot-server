package com.haruhi.botServer.vo;

import com.haruhi.botServer.entity.JmAlbumSqlite;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class JmAlbumManageResp extends JmAlbumSqlite {
    private Boolean zipExists;
    private Boolean pdfExists;
    private String serverZipUrl;
    private String serverPdfUrl;
    private List<JmChapterInfoResp> chapterList;
    private Long imageCount;
    private Long actualImageCount;
}
