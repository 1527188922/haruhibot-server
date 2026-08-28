package com.haruhi.botServer.vo;

import com.haruhi.botServer.entity.JmChapterImageSqlite;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class JmChapterImageManageResp extends JmChapterImageSqlite {
    private Boolean imageFileExists;
    private String imgUrl;
    private String serverImgUrl;
}
