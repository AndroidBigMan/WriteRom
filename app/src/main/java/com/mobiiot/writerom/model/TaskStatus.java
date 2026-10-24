package com.mobiiot.writerom.model;

public enum TaskStatus {
    IDLE("待机"),
    RUNNING("正在填充"),
    PAUSED("已暂停"),
    COMPLETED("已完成"),
    STOPPED("已停止"),
    ERROR("异常中断");

    private final String label;

    TaskStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
