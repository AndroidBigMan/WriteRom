package com.mobiiot.writerom.model;

import androidx.annotation.NonNull;

public class FillConfig {
    private FillMode fillMode;
    private long targetRemainingBytes;
    private int targetPercentage;
    private long targetSpecificSizeBytes;
    private FillContentType contentType;
    private boolean isRandomData;
    private long chunkSizeBytes;
    private boolean importToMediaGallery;
    private int concurrencyThreads;
    private boolean isFragmentationMode;
    private FragmentSizeLevel fragmentSizeLevel;
    private boolean isPunchHolesEnabled;

    public FillConfig() {
        this.fillMode = FillMode.TARGET_REMAINING_SPACE;
        this.targetRemainingBytes = 0L; // 用户要求默认不保留安全空间
        this.targetPercentage = 100;
        this.targetSpecificSizeBytes = 10L * 1024 * 1024 * 1024; // 10GB default
        this.contentType = FillContentType.RAW_FILE;
        this.isRandomData = false; // 默认零字节极速填充
        this.chunkSizeBytes = 500L * 1024 * 1024; // 默认单文件500MB
        this.importToMediaGallery = false; // 方案A默认不污染相册，提供开关
        this.concurrencyThreads = 1; // 默认单线程
        this.isFragmentationMode = false; // 默认顺序大块
        this.fragmentSizeLevel = FragmentSizeLevel.TINY_4K_16K;
        this.isPunchHolesEnabled = false;
    }

    public FillConfig(@NonNull FillConfig other) {
        this.fillMode = other.fillMode;
        this.targetRemainingBytes = other.targetRemainingBytes;
        this.targetPercentage = other.targetPercentage;
        this.targetSpecificSizeBytes = other.targetSpecificSizeBytes;
        this.contentType = other.contentType;
        this.isRandomData = other.isRandomData;
        this.chunkSizeBytes = other.chunkSizeBytes;
        this.importToMediaGallery = other.importToMediaGallery;
        this.concurrencyThreads = other.concurrencyThreads;
        this.isFragmentationMode = other.isFragmentationMode;
        this.fragmentSizeLevel = other.fragmentSizeLevel;
        this.isPunchHolesEnabled = other.isPunchHolesEnabled;
    }

    public int getConcurrencyThreads() {
        return concurrencyThreads;
    }

    public void setConcurrencyThreads(int concurrencyThreads) {
        this.concurrencyThreads = Math.max(1, Math.min(8, concurrencyThreads));
    }

    public boolean isFragmentationMode() {
        return isFragmentationMode;
    }

    public void setFragmentationMode(boolean fragmentationMode) {
        isFragmentationMode = fragmentationMode;
    }

    @NonNull
    public FragmentSizeLevel getFragmentSizeLevel() {
        return fragmentSizeLevel != null ? fragmentSizeLevel : FragmentSizeLevel.TINY_4K_16K;
    }

    public void setFragmentSizeLevel(FragmentSizeLevel fragmentSizeLevel) {
        this.fragmentSizeLevel = fragmentSizeLevel;
    }

    public boolean isPunchHolesEnabled() {
        return isPunchHolesEnabled;
    }

    public void setPunchHolesEnabled(boolean punchHolesEnabled) {
        isPunchHolesEnabled = punchHolesEnabled;
    }

    public FillMode getFillMode() {
        return fillMode;
    }

    public void setFillMode(FillMode fillMode) {
        this.fillMode = fillMode;
    }

    public long getTargetRemainingBytes() {
        return targetRemainingBytes;
    }

    public void setTargetRemainingBytes(long targetRemainingBytes) {
        this.targetRemainingBytes = Math.max(0, targetRemainingBytes);
    }

    public int getTargetPercentage() {
        return targetPercentage;
    }

    public void setTargetPercentage(int targetPercentage) {
        this.targetPercentage = Math.max(1, Math.min(100, targetPercentage));
    }

    public long getTargetSpecificSizeBytes() {
        return targetSpecificSizeBytes;
    }

    public void setTargetSpecificSizeBytes(long targetSpecificSizeBytes) {
        this.targetSpecificSizeBytes = Math.max(0, targetSpecificSizeBytes);
    }

    public FillContentType getContentType() {
        return contentType;
    }

    public void setContentType(FillContentType contentType) {
        this.contentType = contentType;
    }

    public boolean isRandomData() {
        return isRandomData;
    }

    public void setRandomData(boolean randomData) {
        isRandomData = randomData;
    }

    public long getChunkSizeBytes() {
        return chunkSizeBytes;
    }

    public void setChunkSizeBytes(long chunkSizeBytes) {
        this.chunkSizeBytes = Math.max(1024 * 1024, chunkSizeBytes); // 最少1MB
        this.isFragmentationMode = false; // 明确指定分块大小，自动关闭碎片化模式，保证规格一致性
    }

    @NonNull
    public String getEffectiveWriteSpecLabel() {
        if (contentType == FillContentType.IMAGE) {
            return "合规 JPEG 图片";
        } else if (contentType == FillContentType.VIDEO) {
            return "标准 MP4 视频";
        } else if (isFragmentationMode) {
            return "碎片化小文件 (" + getFragmentSizeLevel().getLabel() + ")";
        } else {
            long mb = chunkSizeBytes / (1024 * 1024);
            return mb + "MB 常规分块" + (contentType == FillContentType.MIXED ? " (混合)" : "");
        }
    }

    public boolean isImportToMediaGallery() {
        return importToMediaGallery;
    }

    public void setImportToMediaGallery(boolean importToMediaGallery) {
        this.importToMediaGallery = importToMediaGallery;
    }

    /**
     * 计算在此配置与给定当前存储状态下，本次总计需要填充的字节数
     */
    public long calculateTargetFillBytes(@NonNull StorageInfo storageInfo) {
        long currentFree = storageInfo.getFreeBytes();
        long totalRom = storageInfo.getTotalBytes();
        long currentUsed = storageInfo.getUsedBytes();

        if (currentFree <= 0 || totalRom <= 0) {
            return 0;
        }

        long plannedFillBytes;
        switch (fillMode) {
            case TARGET_REMAINING_SPACE:
                // 目标是让剩余空间降为 targetRemainingBytes
                if (currentFree > targetRemainingBytes) {
                    plannedFillBytes = currentFree - targetRemainingBytes;
                } else {
                    plannedFillBytes = 0;
                }
                break;

            case TARGET_PERCENTAGE:
                // 目标占用比例为 targetPercentage
                long desiredUsed = (long) (totalRom * (targetPercentage / 100.0));
                if (desiredUsed > currentUsed) {
                    plannedFillBytes = desiredUsed - currentUsed;
                } else {
                    plannedFillBytes = 0;
                }
                break;

            case TARGET_SPECIFIC_SIZE:
                plannedFillBytes = targetSpecificSizeBytes;
                break;

            default:
                plannedFillBytes = currentFree;
                break;
        }

        // 绝不能超过手机本身当前剩余可用ROM大小！
        return Math.min(plannedFillBytes, currentFree);
    }
}
