package com.mobiiot.writerom.engine;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
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
            File[] files = dir.listFiles();

            int deletedCount = 0;
            long bytesReclaimed = 0;
            List<String> deletedPaths = new ArrayList<>();

            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && (file.getName().startsWith("filler_") || file.getName().startsWith("temp_"))) {
                        long len = file.length();
                        String path = file.getAbsolutePath();
                        if (file.delete()) {
                            deletedCount++;
                            bytesReclaimed += len;
                            deletedPaths.add(path);

                            final int currentCount = deletedCount;
                            final long currentReclaimed = bytesReclaimed;
                            if (callback != null && currentCount % 10 == 0) {
                                mainHandler.post(() -> callback.onCleanupProgress(currentCount, currentReclaimed));
                            }
                        }
                    }
                }
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

            final int finalTotal = deletedCount;
            final long finalBytes = bytesReclaimed;
            if (callback != null) {
                mainHandler.post(() -> callback.onCleanupCompleted(finalTotal, finalBytes));
            }
        });
    }

    public int countExistingFillerFiles() {
        File dir = StorageHelper.getDedicatedStorageDir(context);
        File[] files = dir.listFiles();
        int count = 0;
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && (file.getName().startsWith("filler_") || file.getName().startsWith("temp_"))) {
                    count++;
                }
            }
        }
        return count;
    }
}
