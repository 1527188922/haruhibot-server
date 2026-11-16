package com.haruhi.botserver.features.ai.model.admin;

import lombok.Data;

/**
 * 会话详情类请求：id + 可选条数。
 */
@Data
public class AiIdReq {

    private Long id;
    private Integer limit;
}
