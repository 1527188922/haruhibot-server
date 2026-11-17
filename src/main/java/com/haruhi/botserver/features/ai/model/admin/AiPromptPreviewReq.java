package com.haruhi.botserver.features.ai.model.admin;

import lombok.Data;

/**
 * 提示词预览请求；不调用 API、不写会话。
 */
@Data
public class AiPromptPreviewReq {

    private Long selfId;
    /** group / private */
    private String scene;
    private Long targetId;
    /** 为空时走自动解析 */
    private String roleName;
    private String userName;
    private Long userId;
    private String text;
    /** 每行一条：昵称: 内容 */
    private String ambientText;
}
