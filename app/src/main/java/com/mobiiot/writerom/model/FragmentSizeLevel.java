package com.mobiiot.writerom.model;

public enum FragmentSizeLevel {
    TINY_4K_16K("极小碎片 (4KB ~ 16KB)", 4 * 1024, 16 * 1024),
    SMALL_32K_128K("常用碎片 (32KB ~ 128KB)", 32 * 1024, 128 * 1024),
    MIXED_4K_256K("混合高熵 (4KB ~ 256KB)", 4 * 1024, 256 * 1024);

    private final String label;
    private final int minBytes;
    private final int maxBytes;

    FragmentSizeLevel(String label, int minBytes, int maxBytes) {
        this.label = label;
        this.minBytes = minBytes;
        this.maxBytes = maxBytes;
    }

    public String getLabel() {
        return label;
    }

    public int getMinBytes() {
        return minBytes;
    }

    public int getMaxBytes() {
        return maxBytes;
    }
}
