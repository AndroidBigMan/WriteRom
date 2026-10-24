package com.mobiiot.writerom.utils;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.provider.MediaStore;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.mobiiot.writerom.model.StorageInfo;
import java.io.File;
import java.io.IOException;
import java.util.Locale;

public class StorageHelper {

    private static final String DIR_NAME = "WriteRom_Data";

    @NonNull
    public static StorageInfo getStorageInfo(@NonNull Context context) {
        try {
            File path = Environment.getDataDirectory();
            StatFs stat = new StatFs(path.getPath());
            long blockSize = stat.getBlockSizeLong();
            long totalBlocks = stat.getBlockCountLong();
            long availableBlocks = stat.getAvailableBlocksLong();

            long totalBytes = totalBlocks * blockSize;
            long freeBytes = availableBlocks * blockSize;

            // 部分系统外置共享分区在 /storage/emulated/0
            File ext = Environment.getExternalStorageDirectory();
            if (ext != null && ext.exists()) {
                StatFs extStat = new StatFs(ext.getPath());
                long extTotal = extStat.getBlockCountLong() * extStat.getBlockSizeLong();
                long extFree = extStat.getAvailableBlocksLong() * extStat.getBlockSizeLong();
                // 如果外置存储容量大于/data（有些设备分开分区），以共享存储为准
                if (extTotal > totalBytes) {
                    totalBytes = extTotal;
                    freeBytes = extFree;
                }
            }

            return new StorageInfo(totalBytes, freeBytes);
        } catch (Exception e) {
            e.printStackTrace();
            return new StorageInfo(0, 0);
        }
    }

    @NonNull
    public static File getDedicatedStorageDir(@NonNull Context context) {
        File dir;
        boolean hasAllFilesAccess = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            hasAllFilesAccess = Environment.isExternalStorageManager();
        }

        if (hasAllFilesAccess || Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            File root = Environment.getExternalStorageDirectory();
            dir = new File(root, DIR_NAME);
        } else {
            // 分区存储严格模式下使用专属外部私有目录
            dir = context.getExternalFilesDir(DIR_NAME);
            if (dir == null) {
                dir = new File(context.getFilesDir(), DIR_NAME);
            }
        }

        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    public static void setFolderMediaVisibility(@NonNull File dir, boolean visibleInGallery) {
        File noMedia = new File(dir, ".nomedia");
        if (visibleInGallery) {
            if (noMedia.exists()) {
                noMedia.delete();
            }
        } else {
            if (!noMedia.exists()) {
                try {
                    noMedia.createNewFile();
                } catch (IOException ignored) {
                }
            }
        }
    }

    public static void scanMediaFile(@NonNull Context context, @NonNull File file, @Nullable String mimeType) {
        try {
            MediaScannerConnection.scanFile(
                    context.getApplicationContext(),
                    new String[]{file.getAbsolutePath()},
                    mimeType != null ? new String[]{mimeType} : null,
                    null
            );
        } catch (Exception ignored) {
        }
    }

    public static void registerImageToMediaStore(@NonNull Context context, @NonNull File file) {
        try {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, file.getName());
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
            values.put(MediaStore.Images.Media.DATA, file.getAbsolutePath());
            values.put(MediaStore.Images.Media.SIZE, file.length());
            values.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000);
            context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        } catch (Exception e) {
            // Android 10+ 可能会限制直接写入 DATA，降级使用扫描
            scanMediaFile(context, file, "image/jpeg");
        }
    }

    public static void registerVideoToMediaStore(@NonNull Context context, @NonNull File file) {
        try {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Video.Media.DISPLAY_NAME, file.getName());
            values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
            values.put(MediaStore.Video.Media.DATA, file.getAbsolutePath());
            values.put(MediaStore.Video.Media.SIZE, file.length());
            values.put(MediaStore.Video.Media.DATE_ADDED, System.currentTimeMillis() / 1000);
            context.getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);
        } catch (Exception e) {
            scanMediaFile(context, file, "video/mp4");
        }
    }

    @NonNull
    public static String formatSize(long bytes) {
        if (bytes <= 0) return "0 B";
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));
        if (digitGroups >= units.length) digitGroups = units.length - 1;
        return String.format(Locale.US, "%.2f %s", bytes / Math.pow(1024, digitGroups), units[digitGroups]);
    }

    @NonNull
    public static String formatSpeed(double bytesPerSec) {
        if (bytesPerSec <= 0) return "0 KB/s";
        if (bytesPerSec < 1024 * 1024) {
            return String.format(Locale.US, "%.1f KB/s", bytesPerSec / 1024.0);
        } else if (bytesPerSec < 1024L * 1024 * 1024) {
            return String.format(Locale.US, "%.2f MB/s", bytesPerSec / (1024.0 * 1024.0));
        } else {
            return String.format(Locale.US, "%.2f GB/s", bytesPerSec / (1024.0 * 1024.0 * 1024.0));
        }
    }

    @NonNull
    public static String formatTime(long totalSec) {
        if (totalSec < 0) totalSec = 0;
        long hours = totalSec / 3600;
        long minutes = (totalSec % 3600) / 60;
        long seconds = totalSec % 60;
        if (hours > 0) {
            return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format(Locale.US, "%02d:%02d", minutes, seconds);
        }
    }
}
