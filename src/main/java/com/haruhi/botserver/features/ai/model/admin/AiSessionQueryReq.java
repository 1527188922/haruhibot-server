package com.haruhi.botserver.features.ai.model.admin;

import com.haruhi.botserver.shared.model.PageReq;
import lombok.Data;

import java.util.List;

/**
 * WebUI 会话列表筛选条件。
 */
@Data
public class AiSessionQueryReq extends PageReq {

    private Long selfId;
    /** group / private */
    private String scene;
    private Long targetId;
    private String roleName;
    /** active / paused */
    private String status;
    /** last_active_at 范围，格式 yyyy-MM-dd HH:mm:ss */
    private List<String> datetimerange;
}
