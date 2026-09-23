package com.haruhi.botserver.features.jmcomic.model;

import com.haruhi.botserver.features.jmcomic.persistence.entity.JmChapterImageSqlite;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class JmChapterImageManageResp extends JmChapterImageSqlite {
    private Boolean imageFileExists;
    private String imgUrl;
    private String serverImgUrl;
}
