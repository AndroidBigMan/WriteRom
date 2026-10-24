package com.mobiiot.writerom.engine;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.mobiiot.writerom.generator.FileGenerator;
import com.mobiiot.writerom.generator.FragmentationGenerator;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class FillEngine {

    public interface OnProgressListener {
        void onProgressUpdate(@NonNull FillProgress progress, @NonNull StorageInfo storageInfo);
        void onStatusChanged(@NonNull TaskStatus status, @Nullable String message);
    }

    private final Context context;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService masterExecutor = Executors.newSingleThreadExecutor();
    private ExecutorService workerPool;

    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final AtomicBoolean isPaused = new AtomicBoolean(false);
    private final AtomicBoolean isCancelled = new AtomicBoolean(false);

    private volatile TaskStatus currentStatus = TaskStatus.IDLE;
    private OnProgressListener listener;

    private final AtomicLong bytesWrittenTotal = new AtomicLong(0);
    private final AtomicInteger filesCreatedCount = new AtomicInteger(0);
    private final AtomicLong globalFileIndex = new AtomicLong(0);

    private volatile long targetTotalBytes = 0;
    private volatile int activeThreads = 1;
    private volatile long startTimeMillis = 0;
    private volatile long lastSpeedSampleTime = 0;
    private volatile long lastSpeedSampleBytes = 0;
    private volatile double currentSpeed = 0;
    private volatile String currentFileName = "";

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

        bytesWrittenTotal.set(0);
        filesCreatedCount.set(0);
        globalFileIndex.set(0);
        currentSpeed = 0;
        currentFileName = "";
        activeThreads = Math.max(1, Math.min(8, config.getConcurrencyThreads()));
        startTimeMillis = SystemClock.elapsedRealtime();
        lastSpeedSampleTime = startTimeMillis;
        lastSpeedSampleBytes = 0;

        notifyStatusChanged(TaskStatus.RUNNING, "正在启动存储填充任务 (" + activeThreads + " 线程 · " + config.getEffectiveWriteSpecLabel() + ")...");

        masterExecutor.execute(() -> runFillTask(config));
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
            lastSpeedSampleBytes = bytesWrittenTotal.get();
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
            if (workerPool != null) {
                workerPool.shutdownNow();
            }
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

        final int threadCount = Math.max(1, Math.min(8, config.getConcurrencyThreads()));
        activeThreads = threadCount;
        workerPool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        try {
            for (int i = 0; i < threadCount; i++) {
                final int workerId = i + 1;
                workerPool.execute(() -> {
                    try {
                        runWorker(workerId, config, dir, initialStorage);
                    } finally {
                        latch.countDown();
                    }
                });
            }

            // 等待所有工作线程完成
            latch.await();

            isRunning.set(false);
            if (isCancelled.get()) {
                currentStatus = TaskStatus.STOPPED;
                notifyStatusChanged(TaskStatus.STOPPED, "任务已由用户手动停止");
            } else {
                currentStatus = TaskStatus.COMPLETED;
                StorageInfo finalStorage = StorageHelper.getStorageInfo(context);
                String reason = finalStorage.getFreeBytes() <= 0 ? "ROM已极限填满 (磁盘满)" : "已达到设定的填充目标";
                notifyStatusChanged(TaskStatus.COMPLETED, reason);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            currentStatus = TaskStatus.STOPPED;
            notifyStatusChanged(TaskStatus.STOPPED, "任务被中断");
        } catch (Exception e) {
            isRunning.set(false);
            currentStatus = TaskStatus.ERROR;
            notifyStatusChanged(TaskStatus.ERROR, "填充出错: " + e.getMessage());
        } finally {
            if (workerPool != null && !workerPool.isShutdown()) {
                workerPool.shutdownNow();
            }
            dispatchProgress(StorageHelper.getStorageInfo(context));
        }
    }

    private void runWorker(int workerId, FillConfig config, File dir, StorageInfo initialStorage) {
        FileGenerator.ProgressCallback callback = new FileGenerator.ProgressCallback() {
            @Override
            public boolean shouldCancel() {
                return isCancelled.get() || bytesWrittenTotal.get() >= targetTotalBytes;
            }

            @Override
            public boolean isPaused() {
                return isPaused.get();
            }

            @Override
            public void onBytesWritten(long deltaBytes) {
                recordBytesWritten(deltaBytes, initialStorage);
            }
        };

        while (!isCancelled.get() && bytesWrittenTotal.get() < targetTotalBytes) {
            while (isPaused.get() && !isCancelled.get()) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }

            if (isCancelled.get() || bytesWrittenTotal.get() >= targetTotalBytes) {
                break;
            }

            try {
                if (config.isFragmentationMode() && (config.getContentType() == FillContentType.RAW_FILE || config.getContentType() == FillContentType.MIXED)) {
                    // 碎片化海量小文件模式 (仅作用于数据文件，保证与多媒体及大分块互斥清晰)
                    int fIndex = (int) globalFileIndex.incrementAndGet();
                    currentFileName = String.format("frag_w%d_%06d.bin", workerId, fIndex);
                    FragmentationGenerator.createFragmentFile(
                            dir,
                            fIndex,
                            workerId,
                            config.getFragmentSizeLevel(),
                            config.isRandomData(),
                            config.isPunchHolesEnabled(),
                            callback
                    );
                    filesCreatedCount.incrementAndGet();
                } else {
                    // 常规顺序大块模式
                    long remainingToFill = targetTotalBytes - bytesWrittenTotal.get();
                    if (remainingToFill <= 0) break;

                    int fIndex = (int) globalFileIndex.incrementAndGet();
                    FillContentType currentType = config.getContentType();
                    if (currentType == FillContentType.MIXED) {
                        int mod = fIndex % 3;
                        if (mod == 1) currentType = FillContentType.RAW_FILE;
                        else if (mod == 2) currentType = FillContentType.IMAGE;
                        else currentType = FillContentType.VIDEO;
                    }

                    File generatedFile = null;
                    switch (currentType) {
                        case RAW_FILE:
                            long chunkSize = Math.min(config.getChunkSizeBytes(), remainingToFill);
                            currentFileName = String.format("filler_w%d_%04d.bin", workerId, fIndex);
                            generatedFile = FileGenerator.createDataFile(dir, chunkSize, config.isRandomData(), fIndex, callback);
                            break;

                        case IMAGE:
                            currentFileName = String.format("filler_w%d_%04d.jpg", workerId, fIndex);
                            generatedFile = ImageGenerator.createImageFile(dir, fIndex, callback);
                            if (config.isImportToMediaGallery()) {
                                StorageHelper.registerImageToMediaStore(context, generatedFile);
                            }
                            break;

                        case VIDEO:
                            currentFileName = String.format("filler_w%d_%04d.mp4", workerId, fIndex);
                            long vidSize = Math.min(20L * 1024 * 1024, remainingToFill);
                            generatedFile = VideoGenerator.createVideoFile(dir, fIndex, vidSize, callback);
                            if (config.isImportToMediaGallery()) {
                                StorageHelper.registerVideoToMediaStore(context, generatedFile);
                            }
                            break;
                    }

                    if (generatedFile != null && generatedFile.exists()) {
                        filesCreatedCount.incrementAndGet();
                    }
                }
            } catch (IOException e) {
                String msg = e.getMessage() != null ? e.getMessage() : "";
                if (msg.contains("ENOSPC") || msg.toLowerCase().contains("no space left")) {
                    // 物理闪存彻底写满
                    isCancelled.set(true);
                    break;
                }
            }
        }
    }

    private synchronized void recordBytesWritten(long deltaBytes, StorageInfo initialStorage) {
        long currentTotal = bytesWrittenTotal.addAndGet(deltaBytes);
        long now = SystemClock.elapsedRealtime();
        long timeDiff = now - lastSpeedSampleTime;

        if (timeDiff >= 400) {
            long bytesDiff = currentTotal - lastSpeedSampleBytes;
            currentSpeed = (bytesDiff * 1000.0) / (double) timeDiff;
            lastSpeedSampleTime = now;
            lastSpeedSampleBytes = currentTotal;

            // 动态推算剩余可用空间并分发
            long dynamicFree = Math.max(0, initialStorage.getFreeBytes() - currentTotal);
            StorageInfo realTimeStorage = new StorageInfo(initialStorage.getTotalBytes(), dynamicFree);
            dispatchProgress(realTimeStorage);
        }
    }

    private void dispatchProgress(StorageInfo latestStorage) {
        long elapsed = (SystemClock.elapsedRealtime() - startTimeMillis) / 1000;
        long eta = 0;
        long written = bytesWrittenTotal.get();
        if (currentSpeed > 0 && targetTotalBytes > written) {
            eta = (long) ((targetTotalBytes - written) / currentSpeed);
        }

        FillProgress progress = new FillProgress(
                currentStatus,
                written,
                targetTotalBytes,
                currentSpeed,
                elapsed,
                eta,
                filesCreatedCount.get(),
                activeThreads,
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
