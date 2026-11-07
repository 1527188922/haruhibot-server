package com.haruhi.botserver.integration.onebot.model.event;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 请求事件（加好友/加群）
 * 文档：https://napneko.github.io/onebot/event#请求事件
 */
@Data
@NoArgsConstructor
public class RequestEvent extends BaseEvent implements Serializable {

    /**
     * friend/group
     */
    private String requestType;
    /**
     * add/invite
     */
    private String subType;
    /**
     * 请求标识，用于同意/拒绝请求
     */
    private String flag;
    private Long userId;
    private Long groupId;
    private String comment;

    public boolean isGroupRequest() {
        return groupId != null;
    }
}
