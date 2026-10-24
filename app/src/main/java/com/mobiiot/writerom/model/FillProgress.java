package com.mobiiot.writerom.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.mobiiot.writerom.utils.StorageHelper;

public class FillProgress {
    private final TaskStatus status;
    private final long bytesWrittenThisSession;
    private final long targetTotalBytes;
    private final double speedBytesPerSec;
    private final long elapsedTimeSec;
    private final long etaSec;
    private final int filesCreatedCount;
    private final int activeThreads;
    private final String currentFileName;
    private final String errorMessage;

    public FillProgress(TaskStatus status,
                        long bytesWrittenThisSession,
                        long targetTotalBytes,
                        double speedBytesPerSec,
                        long elapsedTimeSec,
                        long etaSec,
                        int filesCreatedCount,
                        int activeThreads,
                        @Nullable String currentFileName,
                        @Nullable String errorMessage) {
        this.status = status != null ? status : TaskStatus.IDLE;
        this.bytesWrittenThisSession = Math.max(0, bytesWrittenThisSession);
        this.targetTotalBytes = Math.max(0, targetTotalBytes);
        this.speedBytesPerSec = Math.max(0, speedBytesPerSec);
        this.elapsedTimeSec = Math.max(0, elapsedTimeSec);
        this.etaSec = Math.max(0, etaSec);
        this.filesCreatedCount = Math.max(0, filesCreatedCount);
        this.activeThreads = Math.max(1, activeThreads);
        this.currentFileName = currentFileName != null ? currentFileName : "";
        this.errorMessage = errorMessage != null ? errorMessage : "";
    }

    public FillProgress(TaskStatus status,
                        long bytesWrittenThisSession,
                        long targetTotalBytes,
                        double speedBytesPerSec,
                        long elapsedTimeSec,
                        long etaSec,
                        int filesCreatedCount,
                        @Nullable String currentFileName,
                        @Nullable String errorMessage) {
        this(status, bytesWrittenThisSession, targetTotalBytes, speedBytesPerSec, elapsedTimeSec, etaSec, filesCreatedCount, 1, currentFileName, errorMessage);
    }

    public static FillProgress createIdle() {
        return new FillProgress(TaskStatus.IDLE, 0, 0, 0, 0, 0, 0, 1, "", "");
    }

    @NonNull
    public TaskStatus getStatus() {
        return status;
    }

    public int getActiveThreads() {
        return activeThreads;
    }

    @NonNull
    public FillProgress withStatus(@NonNull TaskStatus newStatus) {
        return new FillProgress(
                newStatus,
                this.bytesWrittenThisSession,
                this.targetTotalBytes,
                newStatus == TaskStatus.PAUSED ? 0 : this.speedBytesPerSec,
                this.elapsedTimeSec,
                this.etaSec,
                this.filesCreatedCount,
                this.activeThreads,
                this.currentFileName,
                this.errorMessage
        );
    }

    public long getBytesWrittenThisSession() {
        return bytesWrittenThisSession;
    }

    public long getTargetTotalBytes() {
        return targetTotalBytes;
    }

    public double getSpeedBytesPerSec() {
        return speedBytesPerSec;
    }

    public long getElapsedTimeSec() {
        return elapsedTimeSec;
    }

    public long getEtaSec() {
        return etaSec;
    }

    public int getFilesCreatedCount() {
        return filesCreatedCount;
    }

    @NonNull
    public String getCurrentFileName() {
        return currentFileName;
    }

    @NonNull
    public String getErrorMessage() {
        return errorMessage;
    }

    public float getProgressPercent() {
        if (targetTotalBytes <= 0) return 0f;
        float percent = ((float) bytesWrittenThisSession / (float) targetTotalBytes) * 100f;
        return Math.min(100f, Math.max(0f, percent));
    }

    @NonNull
    public String getFormattedSpeed() {
        return StorageHelper.formatSpeed(speedBytesPerSec);
    }

    @NonNull
    public String getFormattedWritten() {
        return StorageHelper.formatSize(bytesWrittenThisSession);
    }

    @NonNull
    public String getFormattedTarget() {
        return StorageHelper.formatSize(targetTotalBytes);
    }

    @NonNull
    public String getFormattedElapsed() {
        return StorageHelper.formatTime(elapsedTimeSec);
    }

    @NonNull
    public String getFormattedEta() {
        if (etaSec <= 0 || speedBytesPerSec <= 0) return "--:--";
        return StorageHelper.formatTime(etaSec);
    }
}
