package com.mobiiot.writerom.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import com.mobiiot.writerom.MainActivity;
import com.mobiiot.writerom.R;
import com.mobiiot.writerom.engine.FillEngine;
import com.mobiiot.writerom.model.FillConfig;
import com.mobiiot.writerom.model.FillProgress;
import com.mobiiot.writerom.model.StorageInfo;
import com.mobiiot.writerom.model.TaskStatus;
import com.mobiiot.writerom.ui.hud.FloatingHudManager;

public class StorageFillerService extends Service {

    public static final String ACTION_START = "com.mobiiot.writerom.action.START";
    public static final String ACTION_STOP = "com.mobiiot.writerom.action.STOP";
    public static final String CHANNEL_ID = "storage_filler_service_channel";
    private static final int NOTIFICATION_ID = 1001;

    public class LocalBinder extends Binder {
        @NonNull
        public StorageFillerService getService() {
            return StorageFillerService.this;
        }
    }

    private final IBinder binder = new LocalBinder();
    private FillEngine fillEngine;
    private PowerManager.WakeLock wakeLock;
    private NotificationManager notificationManager;

    @Override
    public void onCreate() {
        super.onCreate();
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannel();

        fillEngine = new FillEngine(this);

        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (powerManager != null) {
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "WriteRom::FillWakeLock");
        }

        // 初始化悬浮窗后台控制监听（当 MainActivity 处于后台或暂停时接管控制）
        FloatingHudManager.getInstance(this).setActionListener(new FloatingHudManager.HudActionListener() {
            @Override
            public void onTogglePause() {
                if (fillEngine != null) {
                    if (fillEngine.isPaused()) {
                        resumeFill();
                    } else if (fillEngine.isRunning()) {
                        pauseFill();
                    }
                }
            }

            @Override
            public void onStopTask() {
                stopFill();
            }

            @Override
            public void onHudVisibilityChanged(boolean isVisible) {
            }
        });
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (ACTION_STOP.equals(action)) {
                stopFill();
            }
        }
        return START_STICKY;
    }

    public void startFill(@NonNull FillConfig config, @Nullable FillEngine.OnProgressListener externalListener) {
        if (wakeLock != null && !wakeLock.isHeld()) {
            wakeLock.acquire(4 * 3600 * 1000L /* 4 hours max */);
        }

        Notification notification = buildNotification("ROM 存储填充中", "正在准备填充...", 0, 0);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }

        fillEngine.setListener(new FillEngine.OnProgressListener() {
            @Override
            public void onProgressUpdate(@NonNull FillProgress progress, @NonNull StorageInfo storageInfo) {
                if (fillEngine.isRunning() && (progress.getStatus() == TaskStatus.RUNNING || progress.getStatus() == TaskStatus.PAUSED)) {
                    updateNotification(progress);
                }
                FloatingHudManager.getInstance(StorageFillerService.this).updateProgress(progress, storageInfo);
                if (externalListener != null) {
                    externalListener.onProgressUpdate(progress, storageInfo);
                }
            }

            @Override
            public void onStatusChanged(@NonNull TaskStatus status, @Nullable String message) {
                if (status == TaskStatus.COMPLETED || status == TaskStatus.STOPPED || status == TaskStatus.ERROR) {
                    stopForegroundNotification();
                }
                if (externalListener != null) {
                    externalListener.onStatusChanged(status, message);
                }
            }
        });

        fillEngine.start(config);
    }

    public void pauseFill() {
        if (fillEngine != null) {
            fillEngine.pause();
        }
    }

    public void resumeFill() {
        if (fillEngine != null) {
            fillEngine.resume();
        }
    }

    public void stopFill() {
        if (fillEngine != null) {
            fillEngine.stop();
        }
        stopForegroundNotification();
    }

    @NonNull
    public FillEngine getFillEngine() {
        return fillEngine;
    }

    private synchronized void stopForegroundNotification() {
        releaseWakeLock();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE);
        } else {
            stopForeground(true);
        }
        if (notificationManager != null) {
            notificationManager.cancel(NOTIFICATION_ID);
        }
    }

    private void updateNotification(FillProgress progress) {
        if (fillEngine == null || !fillEngine.isRunning()) {
            return;
        }
        TaskStatus status = progress.getStatus();
        if (status != TaskStatus.RUNNING && status != TaskStatus.PAUSED) {
            return;
        }

        int max = 100;
        int current = (int) progress.getProgressPercent();
        String contentText = String.format("已写入: %s / %s | %s",
                progress.getFormattedWritten(),
                progress.getFormattedTarget(),
                progress.getFormattedSpeed());

        String title = status == TaskStatus.PAUSED
                ? "ROM 存储填充 (已暂停)"
                : String.format("ROM 填充中 (%d线程%s · %s)",
                    progress.getActiveThreads(),
                    progress.isSlcExhausted() ? " · SLC耗尽" : "",
                    progress.getFormattedSpeed());

        Notification notification = buildNotification(title, contentText, max, current);
        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID, notification);
        }
    }

    private Notification buildNotification(String title, String content, int maxProgress, int currentProgress) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Intent stopIntent = new Intent(this, StorageFillerService.class);
        stopIntent.setAction(ACTION_STOP);
        PendingIntent stopPendingIntent = PendingIntent.getService(
                this, 1, stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(content)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "停止任务", stopPendingIntent);

        if (maxProgress > 0) {
            builder.setProgress(maxProgress, currentProgress, false);
        } else {
            builder.setProgress(0, 0, true);
        }

        return builder.build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "ROM填充后台服务",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("用于显示ROM填充实时进度与速度");
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private void releaseWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopForegroundNotification();
        if (fillEngine != null) {
            fillEngine.stop();
        }
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        if (fillEngine != null && !fillEngine.isRunning()) {
            stopForegroundNotification();
            stopSelf();
        }
    }
}
