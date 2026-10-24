package com.mobiiot.writerom.model;

public enum FillMode {
    TARGET_REMAINING_SPACE("保留剩余空间"),
    TARGET_PERCENTAGE("目标占用比例"),
    TARGET_SPECIFIC_SIZE("指定填充大小");

    private final String displayName;

    FillMode(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
