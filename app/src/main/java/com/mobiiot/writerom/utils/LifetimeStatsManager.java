package com.mobiiot.writerom.utils;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import java.util.Locale;

public class LifetimeStatsManager {

    private static final String PREFS_NAME = "writerom_lifetime_stats";
    private static final String KEY_LIFETIME_BYTES = "key_lifetime_bytes_written";

    private LifetimeStatsManager() {}

    public static long getLifetimeBytesWritten(@NonNull Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return sp.getLong(KEY_LIFETIME_BYTES, 0L);
    }

    public static synchronized void addBytesWritten(@NonNull Context context, long deltaBytes) {
        if (deltaBytes <= 0) return;
        SharedPreferences sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long current = sp.getLong(KEY_LIFETIME_BYTES, 0L);
        sp.edit().putLong(KEY_LIFETIME_BYTES, current + deltaBytes).apply();
    }

    public static double estimatePeCycles(long lifetimeBytes, long totalRomBytes) {
        if (totalRomBytes <= 0) return 0.0;
        return (double) lifetimeBytes / (double) totalRomBytes;
    }

    @NonNull
    public static String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format(Locale.getDefault(), "%.1f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format(Locale.getDefault(), "%.2f MB", bytes / (1024.0 * 1024.0));
        } else if (bytes < 1024L * 1024L * 1024L * 1024L) {
            return String.format(Locale.getDefault(), "%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        } else {
            return String.format(Locale.getDefault(), "%.3f TB", bytes / (1024.0 * 1024.0 * 1024.0 * 1024.0));
        }
    }
}
