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
    /**
     * 该漫画所属的收藏夹id列表。
     * 用于前端回显"已收藏到哪些收藏夹"，空列表表示未收藏到任何收藏夹。
     * 与 collected 字段的关系：collected=true 等价于 favoriteIds 非空。
     */
    private List<Long> favoriteIds;
    /**
     * 加入"当前查询的收藏夹"的时间（yyyy-MM-dd HH:mm:ss）。
     * 只在按 favoriteId 查询收藏夹时有值，其余场景为null
     */
    private String favoriteAddTime;
}
