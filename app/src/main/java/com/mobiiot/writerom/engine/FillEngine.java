package com.mobiiot.writerom.engine;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.mobiiot.writerom.generator.FileGenerator;
import com.mobiiot.writerom.generator.ImageGenerator;
import com.mobiiot.writerom.generator.VideoGenerator;
import com.mobiiot.writerom.model.FillConfig;
import com.mobiiot.writerom.model.FillContentType;
import com.mobiiot.writerom.model.FillProgress;
import com.mobiiot.writerom.model.StorageInfo;
import com.mobiiot.writerom.model.TaskStatus;
import com.mobiiot.writerom.utils.StorageHelper;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class FillEngine {

    public interface OnProgressListener {
        void onProgressUpdate(@NonNull FillProgress progress, @NonNull StorageInfo storageInfo);
        void onStatusChanged(@NonNull TaskStatus status, @Nullable String message);
    }

    private final Context context;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final AtomicBoolean isPaused = new AtomicBoolean(false);
    private final AtomicBoolean isCancelled = new AtomicBoolean(false);

    private volatile TaskStatus currentStatus = TaskStatus.IDLE;
    private OnProgressListener listener;

    private long bytesWrittenTotal = 0;
    private long targetTotalBytes = 0;
    private int filesCreatedCount = 0;
    private long startTimeMillis = 0;
    private long lastSpeedSampleTime = 0;
    private long lastSpeedSampleBytes = 0;
    private double currentSpeed = 0;
    private String currentFileName = "";

    public FillEngine(@NonNull Context context) {
        this.context = context.getApplicationContext();
    }

    public void setListener(@Nullable OnProgressListener listener) {
        this.listener = listener;
    }

    public synchronized boolean isRunning() {
        return isRunning.get();
    }

    public synchronized boolean isPaused() {
        return isPaused.get();
    }

    @NonNull
    public TaskStatus getCurrentStatus() {
        return currentStatus;
    }

    public synchronized void start(@NonNull FillConfig config) {
        if (isRunning.get()) {
            return;
        }

        isRunning.set(true);
        isPaused.set(false);
        isCancelled.set(false);
        currentStatus = TaskStatus.RUNNING;

        bytesWrittenTotal = 0;
        filesCreatedCount = 0;
        currentSpeed = 0;
        currentFileName = "";
        startTimeMillis = SystemClock.elapsedRealtime();
        lastSpeedSampleTime = startTimeMillis;
        lastSpeedSampleBytes = 0;

        notifyStatusChanged(TaskStatus.RUNNING, "正在启动存储填充任务...");

        executor.execute(() -> runFillTask(config));
    }

    public synchronized void pause() {
        if (isRunning.get() && !isPaused.get()) {
            isPaused.set(true);
            currentStatus = TaskStatus.PAUSED;
            currentSpeed = 0;
            notifyStatusChanged(TaskStatus.PAUSED, "任务已暂停");
            dispatchProgress(StorageHelper.getStorageInfo(context));
        }
    }

    public synchronized void resume() {
        if (isRunning.get() && isPaused.get()) {
            isPaused.set(false);
            currentStatus = TaskStatus.RUNNING;
            lastSpeedSampleTime = SystemClock.elapsedRealtime();
            lastSpeedSampleBytes = bytesWrittenTotal;
            notifyStatusChanged(TaskStatus.RUNNING, "任务已继续");
            dispatchProgress(StorageHelper.getStorageInfo(context));
        }
    }

    public synchronized void stop() {
        if (isRunning.get()) {
            isCancelled.set(true);
            isPaused.set(false);
            currentStatus = TaskStatus.STOPPED;
            currentSpeed = 0;
            notifyStatusChanged(TaskStatus.STOPPED, "任务正在停止...");
            dispatchProgress(StorageHelper.getStorageInfo(context));
        }
    }

    private void runFillTask(@NonNull FillConfig config) {
        File dir = StorageHelper.getDedicatedStorageDir(context);
        StorageHelper.setFolderMediaVisibility(dir, config.isImportToMediaGallery());

        StorageInfo initialStorage = StorageHelper.getStorageInfo(context);
        targetTotalBytes = config.calculateTargetFillBytes(initialStorage);

        if (targetTotalBytes <= 0) {
            isRunning.set(false);
            currentStatus = TaskStatus.COMPLETED;
            notifyStatusChanged(TaskStatus.COMPLETED, "当前已达到或超过目标阈值，无需填充");
            return;
        }

        FileGenerator.ProgressCallback callback = new FileGenerator.ProgressCallback() {
            @Override
            public boolean shouldCancel() {
                return isCancelled.get();
            }

            @Override
            public boolean isPaused() {
                return isPaused.get();
            }

            @Override
            public void onBytesWritten(long deltaBytes) {
                bytesWrittenTotal += deltaBytes;
                long now = SystemClock.elapsedRealtime();
                long timeDiff = now - lastSpeedSampleTime;

                if (timeDiff >= 400) { // 每 400ms 刷新一次瞬时速度与实时存储状态
                    long bytesDiff = bytesWrittenTotal - lastSpeedSampleBytes;
                    currentSpeed = (bytesDiff * 1000.0) / (double) timeDiff;
                    lastSpeedSampleTime = now;
                    lastSpeedSampleBytes = bytesWrittenTotal;

                    // 动态计算当前可用剩余空间，驱动手机 ROM 存储状态模块实时刷新
                    long dynamicFree = Math.max(0, initialStorage.getFreeBytes() - bytesWrittenTotal);
                    StorageInfo realTimeStorage = new StorageInfo(initialStorage.getTotalBytes(), dynamicFree);
                    dispatchProgress(realTimeStorage);
                }
            }
        };

        int fileIndex = 1;
        String completionReason = "填充目标已达成";

        try {
            while (!isCancelled.get()) {
                StorageInfo currentStorage = StorageHelper.getStorageInfo(context);

                // 检查是否已达到目标
                long remainingToFill = targetTotalBytes - bytesWrittenTotal;
                if (remainingToFill <= 0 || currentStorage.getFreeBytes() <= 0) {
                    completionReason = currentStorage.getFreeBytes() <= 0 ? "ROM已完全填满" : "已达到设定的填充目标";
                    break;
                }

                while (isPaused.get() && !isCancelled.get()) {
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }

                if (isCancelled.get()) break;

                // 确定当前批次生成的文件类型
                FillContentType currentType = config.getContentType();
                if (currentType == FillContentType.MIXED) {
                    int mod = fileIndex % 3;
                    if (mod == 1) currentType = FillContentType.RAW_FILE;
                    else if (mod == 2) currentType = FillContentType.IMAGE;
                    else currentType = FillContentType.VIDEO;
                }

                File generatedFile = null;
                try {
                    switch (currentType) {
                        case RAW_FILE:
                            long chunkSize = Math.min(config.getChunkSizeBytes(), remainingToFill);
                            currentFileName = String.format("filler_data_%04d.bin", fileIndex);
                            generatedFile = FileGenerator.createDataFile(dir, chunkSize, config.isRandomData(), fileIndex, callback);
                            break;

                        case IMAGE:
                            currentFileName = String.format("filler_img_%04d.jpg", fileIndex);
                            generatedFile = ImageGenerator.createImageFile(dir, fileIndex, callback);
                            if (config.isImportToMediaGallery()) {
                                StorageHelper.registerImageToMediaStore(context, generatedFile);
                            }
                            break;

                        case VIDEO:
                            currentFileName = String.format("filler_vid_%04d.mp4", fileIndex);
                            // 默认生成适中短视频，上限单文件20MB或单块大小
                            long vidSize = Math.min(20L * 1024 * 1024, remainingToFill);
                            generatedFile = VideoGenerator.createVideoFile(dir, fileIndex, vidSize, callback);
                            if (config.isImportToMediaGallery()) {
                                StorageHelper.registerVideoToMediaStore(context, generatedFile);
                            }
                            break;
                    }
                } catch (IOException e) {
                    // 捕获 ENOSPC "No space left on device" 或空间写满异常
                    String msg = e.getMessage() != null ? e.getMessage() : "";
                    if (msg.contains("ENOSPC") || msg.toLowerCase().contains("no space left") || currentStorage.getFreeBytes() < 1024 * 1024) {
                        completionReason = "手机ROM已极限填满 (磁盘满)";
                        break;
                    } else {
                        throw e; // 其他异常抛出处理
                    }
                }

                if (generatedFile != null && generatedFile.exists()) {
                    filesCreatedCount++;
                }

                fileIndex++;
                // 每生成完一个文件，使用底层文件系统最新容量校准刷新
                StorageInfo afterFileStorage = StorageHelper.getStorageInfo(context);
                dispatchProgress(afterFileStorage);
            }

            isRunning.set(false);
            if (isCancelled.get()) {
                currentStatus = TaskStatus.STOPPED;
                notifyStatusChanged(TaskStatus.STOPPED, "任务已由用户手动停止");
            } else {
                currentStatus = TaskStatus.COMPLETED;
                notifyStatusChanged(TaskStatus.COMPLETED, completionReason);
            }
        } catch (Exception e) {
            isRunning.set(false);
            currentStatus = TaskStatus.ERROR;
            notifyStatusChanged(TaskStatus.ERROR, "填充出错: " + e.getMessage());
        } finally {
            dispatchProgress(StorageHelper.getStorageInfo(context));
        }
    }

    private void dispatchProgress(StorageInfo latestStorage) {
        long elapsed = (SystemClock.elapsedRealtime() - startTimeMillis) / 1000;
        long eta = 0;
        if (currentSpeed > 0 && targetTotalBytes > bytesWrittenTotal) {
            eta = (long) ((targetTotalBytes - bytesWrittenTotal) / currentSpeed);
        }

        FillProgress progress = new FillProgress(
                currentStatus,
                bytesWrittenTotal,
                targetTotalBytes,
                currentSpeed,
                elapsed,
                eta,
                filesCreatedCount,
                currentFileName,
                null
        );

        if (listener != null) {
            mainHandler.post(() -> listener.onProgressUpdate(progress, latestStorage));
        }
    }

    private void notifyStatusChanged(TaskStatus status, String msg) {
        if (listener != null) {
            mainHandler.post(() -> listener.onStatusChanged(status, msg));
        }
    }
}
