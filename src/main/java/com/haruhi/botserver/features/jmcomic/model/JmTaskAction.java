package com.haruhi.botserver.features.jmcomic.model;

import org.apache.commons.lang3.StringUtils;

/**
 * JM长任务的动作类型
 * <p>
 * actionName 与历史 executeWithJmLock 传入的文案保持一致，避免改变已有响应文案
 */
public enum JmTaskAction {

    DOWNLOAD("下载漫画", "download"),
    GENERATE_ZIP("生成zip", "zip"),
    GENERATE_PDF("生成pdf", "pdf"),
    OTHER("其他操作", "other");

    private final String actionName;
    private final String code;

    JmTaskAction(String actionName, String code) {
        this.actionName = actionName;
        this.code = code;
    }

    public String getActionName() {
        return actionName;
    }

    public String getCode() {
        return code;
    }

    /**
     * 兼容历史 String 动作名入口
     */
    public static JmTaskAction fromActionName(String actionName) {
        if (StringUtils.isBlank(actionName)) {
            return OTHER;
        }
        for (JmTaskAction action : values()) {
            if (action.actionName.equals(actionName)) {
                return action;
            }
        }
        return OTHER;
    }
}
