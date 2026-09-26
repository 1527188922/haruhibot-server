package com.haruhi.botserver.features.jmcomic.model;

/**
 * JM任务状态，仅存在于内存中，不持久化
 */
public enum JmTaskStatusEnum {

    QUEUED("排队中"),
    RUNNING("进行中"),
    SUCCESS("已完成"),
    FAIL("失败"),
    CANCELLED("已取消");

    private final String statusName;

    JmTaskStatusEnum(String statusName) {
        this.statusName = statusName;
    }

    public String getStatusName() {
        return statusName;
    }

    public boolean isFinished() {
        return this == SUCCESS || this == FAIL || this == CANCELLED;
    }
}
