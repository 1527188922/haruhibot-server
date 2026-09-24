package com.haruhi.botserver.infrastructure.kvstore.model;

import com.haruhi.botserver.shared.model.PageReq;
import lombok.Data;

@Data
public class KvQuery extends PageReq {
    private String key;
    private String content;
    private String remark;
}
