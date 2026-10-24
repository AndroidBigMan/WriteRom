package com.mobiiot.writerom.engine;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.mobiiot.writerom.utils.StorageHelper;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CleanupManager {

    public interface CleanupCallback {
        void onCleanupStarted();
        void onCleanupProgress(int deletedCount, long bytesReclaimed);
        void onCleanupCompleted(int totalDeleted, long totalBytesReclaimed);
    }

    private final Context context;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public CleanupManager(@NonNull Context context) {
        this.context = context.getApplicationContext();
    }

    public void cleanAllAsync(@Nullable CleanupCallback callback) {
        if (callback != null) {
            mainHandler.post(callback::onCleanupStarted);
        }

        executor.execute(() -> {
            File dir = StorageHelper.getDedicatedStorageDir(context);
            int[] stats = new int[]{0}; // deletedCount
            long[] reclaimed = new long[]{0}; // bytesReclaimed
            List<String> deletedPaths = new ArrayList<>();

            deleteRecursive(dir, dir, stats, reclaimed, deletedPaths, callback);

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
            if (callback != null) {
                mainHandler.post(() -> callback.onCleanupCompleted(finalTotal, finalBytes));
            }
        });
    }

    private void deleteRecursive(File root, File current, int[] stats, long[] reclaimed, List<String> deletedPaths, CleanupCallback callback) {
        File[] files = current.listFiles();
        if (files == null) return;

        for (File f : files) {
            if (f.isDirectory()) {
                if (f.getName().startsWith("frag_bucket_") || f.getName().startsWith("temp_")) {
                    deleteRecursive(root, f, stats, reclaimed, deletedPaths, callback);
                    f.delete();
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

                        if (callback != null && stats[0] % 100 == 0) {
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
        return countRecursive(dir);
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
