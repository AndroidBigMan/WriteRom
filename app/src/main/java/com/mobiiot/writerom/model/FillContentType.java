package com.mobiiot.writerom.model;

public enum FillContentType {
    RAW_FILE("大文件 (零/随机块)"),
    IMAGE("真实图片 (JPG)"),
    VIDEO("真实视频 (MP4)"),
    MIXED("混合模式 (文件/图片/视频)");

    private final String displayName;

    FillContentType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
