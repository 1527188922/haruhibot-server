package com.haruhi.botserver.dictionary.model;

import com.haruhi.botserver.shared.model.PageReq;
import lombok.Data;

@Data
public class DictQueryReq extends PageReq {
    private String key;
    private String content;
    private String remark;
}
