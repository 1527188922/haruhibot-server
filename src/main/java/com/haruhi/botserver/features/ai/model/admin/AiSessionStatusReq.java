package com.haruhi.botserver.features.ai.model.admin;

import lombok.Data;

/**
 * 会话暂停/恢复请求。
 */
@Data
public class AiSessionStatusReq {

    private Long id;
    /** active / paused */
    private String status;
}
