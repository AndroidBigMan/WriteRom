package com.mobiiot.writerom.engine;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.mobiiot.writerom.generator.ContactsGenerator;
import com.mobiiot.writerom.utils.StorageHelper;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class CleanupManager {

    public interface CleanupCallback {
        void onCleanupStarted();
        void onCleanupProgress(int deletedCount, long bytesReclaimed);
        void onCleanupCompleted(int totalDeleted, long totalBytesReclaimed);
        void onCleanupStopped(int totalDeleted, long totalBytesReclaimed);
    }

    private final Context context;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean isCancelled = new AtomicBoolean(false);

    public CleanupManager(@NonNull Context context) {
        this.context = context.getApplicationContext();
    }

    public void stopCleanup() {
        isCancelled.set(true);
    }

    public boolean isCancelled() {
        return isCancelled.get();
    }

    public void cleanAllAsync(@Nullable CleanupCallback callback) {
        isCancelled.set(false);
        if (callback != null) {
            mainHandler.post(callback::onCleanupStarted);
        }

        executor.execute(() -> {
            File dir = StorageHelper.getDedicatedStorageDir(context);
            int[] stats = new int[]{0}; // deletedCount
            long[] reclaimed = new long[]{0}; // bytesReclaimed
            long[] lastReportTime = new long[]{SystemClock.elapsedRealtime()};
            List<String> deletedPaths = new ArrayList<>();

            deleteRecursive(dir, dir, stats, reclaimed, lastReportTime, deletedPaths, callback);

            if (!isCancelled.get()) {
                // 清理所有测试生成的联系人并释放 SQLite 数据空间
                int contactsDeleted = ContactsGenerator.cleanAllContacts(context);
                stats[0] += contactsDeleted;
                reclaimed[0] += contactsDeleted * 2048L;
            }

            // 清理系统 MediaStore 中被删除文件的残留记录
            if (!deletedPaths.isEmpty()) {
                try {
                    MediaScannerConnection.scanFile(
                            context,
                            deletedPaths.toArray(new String[0]),
                            null,
                            null
                    );
                } catch (Exception ignored) {
                }
            }

            final int finalTotal = stats[0];
            final long finalBytes = reclaimed[0];
            final boolean stopped = isCancelled.get();
            if (callback != null) {
                mainHandler.post(() -> {
                    if (stopped) {
                        callback.onCleanupStopped(finalTotal, finalBytes);
                    } else {
                        callback.onCleanupCompleted(finalTotal, finalBytes);
                    }
                });
            }
        });
    }

    private void deleteRecursive(File root, File current, int[] stats, long[] reclaimed, long[] lastReportTime, List<String> deletedPaths, CleanupCallback callback) {
        if (isCancelled.get()) return;

        File[] files = current.listFiles();
        if (files == null) return;

        for (File f : files) {
            if (isCancelled.get()) break;

            if (f.isDirectory()) {
                if (f.getName().startsWith("frag_bucket_") || f.getName().startsWith("temp_")) {
                    deleteRecursive(root, f, stats, reclaimed, lastReportTime, deletedPaths, callback);
                    if (!isCancelled.get()) {
                        f.delete();
                    }
                }
            } else if (f.isFile()) {
                String name = f.getName();
                if (name.startsWith("filler_") || name.startsWith("temp_") || name.startsWith("frag_")) {
                    long len = f.length();
                    String path = f.getAbsolutePath();
                    if (f.delete()) {
                        stats[0]++;
                        reclaimed[0] += len;
                        if (name.endsWith(".jpg") || name.endsWith(".mp4")) {
                            deletedPaths.add(path);
                        }

                        // 高频敏捷进度上报：前 10 个文件每删 1 个报 1 次；100 个以内每 5 个报 1 次；后续每 25 个或每 150ms 上报一次
                        long now = SystemClock.elapsedRealtime();
                        boolean shouldReport = (stats[0] <= 10)
                                || (stats[0] <= 100 && stats[0] % 5 == 0)
                                || (stats[0] % 25 == 0)
                                || (now - lastReportTime[0] >= 150);

                        if (callback != null && shouldReport) {
                            lastReportTime[0] = now;
                            final int cCount = stats[0];
                            final long cBytes = reclaimed[0];
                            mainHandler.post(() -> callback.onCleanupProgress(cCount, cBytes));
                        }
                    }
                }
            }
        }
    }

    public int countExistingFillerFiles() {
        File dir = StorageHelper.getDedicatedStorageDir(context);
        int files = countRecursive(dir);
        int contacts = ContactsGenerator.countExistingContacts(context);
        return files + contacts;
    }

    private int countRecursive(File current) {
        File[] files = current.listFiles();
        if (files == null) return 0;
        int count = 0;
        for (File f : files) {
            if (f.isDirectory() && f.getName().startsWith("frag_bucket_")) {
                count += countRecursive(f);
            } else if (f.isFile()) {
                String name = f.getName();
                if (name.startsWith("filler_") || name.startsWith("temp_") || name.startsWith("frag_")) {
                    count++;
                }
            }
        }
        return count;
    }
}
