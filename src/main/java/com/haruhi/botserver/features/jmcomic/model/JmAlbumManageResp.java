package com.haruhi.botserver.features.jmcomic.model;

import com.haruhi.botserver.features.jmcomic.persistence.entity.JmAlbumSqlite;
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
    /**
     * JM服务器封面图链接，任何时候都有值
     */
    private String coverUrl;
    /**
     * 本地服务器封面图url，本地封面文件(jmcomic/{文件夹}/{jmId}.jpg)不存在时为null
     */
    private String serverCoverUrl;
    private List<JmChapterInfoResp> chapterList;
    private Long imageCount;
    private Long actualImageCount;
}
