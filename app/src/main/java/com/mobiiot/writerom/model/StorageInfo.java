package com.mobiiot.writerom.model;

import androidx.annotation.NonNull;
import com.mobiiot.writerom.utils.StorageHelper;

public class StorageInfo {
    private final long totalBytes;
    private final long freeBytes;
    private final long usedBytes;
    private final double percentUsed;

    public StorageInfo(long totalBytes, long freeBytes) {
        this.totalBytes = Math.max(0, totalBytes);
        this.freeBytes = Math.max(0, freeBytes);
        this.usedBytes = Math.max(0, this.totalBytes - this.freeBytes);
        this.percentUsed = this.totalBytes > 0 ? ((double) this.usedBytes / (double) this.totalBytes) * 100.0 : 0.0;
    }

    public long getTotalBytes() {
        return totalBytes;
    }

    public long getFreeBytes() {
        return freeBytes;
    }

    public long getUsedBytes() {
        return usedBytes;
    }

    public double getPercentUsed() {
        return percentUsed;
    }

    @NonNull
    public String getFormattedTotal() {
        return StorageHelper.formatSize(totalBytes);
    }

    @NonNull
    public String getFormattedFree() {
        return StorageHelper.formatSize(freeBytes);
    }

    @NonNull
    public String getFormattedUsed() {
        return StorageHelper.formatSize(usedBytes);
    }

    @NonNull
    @Override
    public String toString() {
        return "StorageInfo{" +
                "total=" + getFormattedTotal() +
                ", free=" + getFormattedFree() +
                ", used=" + getFormattedUsed() +
                ", percent=" + String.format("%.1f%%", percentUsed) +
                '}';
    }
}
