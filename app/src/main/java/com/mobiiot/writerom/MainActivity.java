package com.mobiiot.writerom;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.provider.Settings;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.activity.ComponentActivity;
import androidx.compose.ui.platform.ComposeView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.mobiiot.writerom.engine.CleanupManager;
import com.mobiiot.writerom.engine.FillEngine;
import com.mobiiot.writerom.model.FillConfig;
import com.mobiiot.writerom.model.FillContentType;
import com.mobiiot.writerom.model.FillMode;
import com.mobiiot.writerom.model.FillProgress;
import com.mobiiot.writerom.model.StorageInfo;
import com.mobiiot.writerom.model.TaskStatus;
import com.mobiiot.writerom.service.StorageFillerService;
import com.mobiiot.writerom.ui.StorageUiState;
import com.mobiiot.writerom.ui.UiBridge;
import com.mobiiot.writerom.ui.hud.FloatingHudManager;
import com.mobiiot.writerom.utils.HardwareMonitor;
import com.mobiiot.writerom.utils.LifetimeStatsManager;
import com.mobiiot.writerom.utils.StorageHelper;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends ComponentActivity implements UiBridge.UiCallbacks {

    private static final int REQUEST_CODE_NOTIF = 101;
    private static final int REQUEST_CODE_ALL_FILES = 102;
    private static final int REQUEST_CODE_OVERLAY = 103;
    private static final int REQUEST_CODE_CONTACTS = 104;

    private ComposeView composeView;
    private UiBridge.BridgeController bridgeController;

    private StorageFillerService storageService;
    private boolean isServiceBound = false;

    private FillConfig fillConfig;
    private StorageInfo currentStorage;
    private FillProgress currentProgress;
    private CleanupManager cleanupManager;
    private HardwareMonitor hardwareMonitor;

    private boolean isCleaning = false;
    private int existingFilesCount = 0;
    private int cleaningDeletedCount = 0;
    private long cleaningReclaimedBytes = 0;
    private String statusMessage = "";

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            StorageFillerService.LocalBinder binder = (StorageFillerService.LocalBinder) service;
            storageService = binder.getService();
            isServiceBound = true;
            syncServiceStatus();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            storageService = null;
            isServiceBound = false;
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        composeView = findViewById(R.id.compose_view);

        // 1. 初始化 Java 业务模型与管理器
        fillConfig = new FillConfig();
        currentStorage = StorageHelper.getStorageInfo(this);
        currentProgress = FillProgress.createIdle();
        cleanupManager = new CleanupManager(this);

        // 2. 初始化 Kotlin ComposeView 桥接 (优先于硬件监听初始化，防止监听回调触发 NPE)
        bridgeController = new UiBridge.BridgeController(this);
        UiBridge.bindComposeView(composeView, bridgeController);

        // 3. 初始化硬件监控并注册监听
        hardwareMonitor = new HardwareMonitor(this);
        hardwareMonitor.setListener((tempCelsius, thermalLevel, thermalStatusLabel) -> updateUiState());
        hardwareMonitor.startMonitoring();

        // 4. 启动并绑定前台服务
        Intent serviceIntent = new Intent(this, StorageFillerService.class);
        startService(serviceIntent);
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);

        // 5. 初始化全局悬浮窗管理器监听与自启判断
        FloatingHudManager hudManager = FloatingHudManager.getInstance(this);
        hudManager.setActionListener(new FloatingHudManager.HudActionListener() {
            @Override
            public void onTogglePause() {
                if (currentProgress.getStatus() == TaskStatus.PAUSED) {
                    onResumeTask();
                } else if (currentProgress.getStatus() == TaskStatus.RUNNING) {
                    onPauseTask();
                }
            }

            @Override
            public void onStopTask() {
                MainActivity.this.onStopTask();
            }

            @Override
            public void onHudVisibilityChanged(boolean isVisible) {
                updateUiState();
            }
        });
        if (hudManager.isHudEnabledPref() && hudManager.canDrawOverlays()) {
            hudManager.showHud();
        }

        // 6. 权限与状态检查
        checkPermissions();
        refreshStorageAndFileCount();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshStorageAndFileCount();
        FloatingHudManager hudManager = FloatingHudManager.getInstance(this);
        if (hudManager.isHudEnabledPref() && hudManager.canDrawOverlays() && !hudManager.isHudShown()) {
            hudManager.showHud();
        }
        updateUiState();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (hardwareMonitor != null) {
            hardwareMonitor.stopMonitoring();
        }
        if (isServiceBound) {
            unbindService(serviceConnection);
            isServiceBound = false;
        }
    }

    private void syncServiceStatus() {
        if (storageService != null) {
            FillEngine engine = storageService.getFillEngine();
            if (engine.isRunning()) {
                currentProgress = new FillProgress(
                        engine.getCurrentStatus(),
                        0,
                        fillConfig.calculateTargetFillBytes(currentStorage),
                        0, 0, 0, 0,
                        fillConfig.getConcurrencyThreads(),
                        "", null
                );
            }
        }
        updateUiState();
    }

    private void refreshStorageAndFileCount() {
        currentStorage = StorageHelper.getStorageInfo(this);
        new Thread(() -> {
            int count = cleanupManager.countExistingFillerFiles();
            runOnUiThread(() -> {
                existingFilesCount = count;
                updateUiState();
            });
        }).start();
    }

    private void updateUiState() {
        boolean hasStoragePermission = checkHasStoragePermission();
        boolean hasNotifPermission = checkHasNotificationPermission();
        boolean hasContactsPermission = checkHasContactsPermission();

        long estimatedFill = fillConfig.calculateTargetFillBytes(currentStorage);
        String formattedEst = StorageHelper.formatSize(estimatedFill);

        List<Float> speedHistoryList = new ArrayList<>();
        if (currentProgress.getSpeedHistory() != null) {
            for (float s : currentProgress.getSpeedHistory()) {
                speedHistoryList.add(s);
            }
        }

        long lifetimeBytes = LifetimeStatsManager.getLifetimeBytesWritten(this) + currentProgress.getBytesWrittenThisSession();
        String formattedLifetime = LifetimeStatsManager.formatBytes(lifetimeBytes);
        double estimatedPe = LifetimeStatsManager.estimatePeCycles(lifetimeBytes, currentStorage.getTotalBytes());

        float temp = hardwareMonitor != null ? hardwareMonitor.getCurrentTempCelsius() : 0f;
        int thermalLevel = hardwareMonitor != null ? hardwareMonitor.getCurrentThermalLevel() : 0;
        String thermalLabel = hardwareMonitor != null ? hardwareMonitor.getCurrentThermalLabel() : "正常";

        StorageUiState uiState = new StorageUiState(
                currentStorage.getTotalBytes(),
                currentStorage.getFreeBytes(),
                currentStorage.getUsedBytes(),
                currentStorage.getPercentUsed(),
                currentStorage.getFormattedTotal(),
                currentStorage.getFormattedFree(),
                currentStorage.getFormattedUsed(),
                fillConfig.getFillMode(),
                fillConfig.getTargetRemainingBytes(),
                fillConfig.getTargetPercentage(),
                fillConfig.getTargetSpecificSizeBytes(),
                fillConfig.getContentType(),
                fillConfig.isRandomData(),
                (int) (fillConfig.getChunkSizeBytes() / (1024 * 1024)),
                fillConfig.isImportToMediaGallery(),
                fillConfig.getConcurrencyThreads(),
                fillConfig.isFragmentationMode(),
                fillConfig.getFragmentSizeLevel(),
                fillConfig.isPunchHolesEnabled(),
                currentProgress.getActiveThreads(),
                estimatedFill,
                formattedEst,
                currentProgress.getStatus(),
                currentProgress.getBytesWrittenThisSession(),
                currentProgress.getTargetTotalBytes(),
                currentProgress.getProgressPercent(),
                currentProgress.getSpeedBytesPerSec(),
                currentProgress.getFormattedSpeed(),
                currentProgress.getFormattedWritten(),
                currentProgress.getFormattedTarget(),
                currentProgress.getFormattedElapsed(),
                currentProgress.getFormattedEta(),
                currentProgress.getFilesCreatedCount(),
                currentProgress.getCurrentFileName(),
                statusMessage,
                hasStoragePermission,
                hasNotifPermission,
                hasContactsPermission,
                isCleaning,
                existingFilesCount,
                cleaningDeletedCount,
                cleaningReclaimedBytes,
                StorageHelper.formatSize(cleaningReclaimedBytes),
                speedHistoryList,
                currentProgress.getPeakSpeedMb(),
                currentProgress.getAvgSpeedMb(),
                currentProgress.isSlcExhausted(),
                temp,
                thermalLevel,
                thermalLabel,
                lifetimeBytes,
                formattedLifetime,
                estimatedPe,
                FloatingHudManager.getInstance(this).isHudShown(),
                FloatingHudManager.getInstance(this).canDrawOverlays()
        );

        if (bridgeController != null) {
            bridgeController.updateState(uiState);
        }
        FloatingHudManager.getInstance(this).updateState(uiState);
    }

    private boolean checkHasStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        } else {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED;
        }
    }

    private boolean checkHasNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    private boolean checkHasContactsPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_CONTACTS)
                == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestContactsPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS},
                REQUEST_CODE_CONTACTS);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_CONTACTS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "通讯录权限已授予", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "未授予通讯录权限，联系人填充将无法执行", Toast.LENGTH_LONG).show();
            }
            updateUiState();
        } else if (requestCode == REQUEST_CODE_NOTIF) {
            updateUiState();
        }
    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!checkHasNotificationPermission()) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_CODE_NOTIF);
            }
        }
    }

    // ==================== UiBridge.UiCallbacks 接口实现 ====================

    @Override
    public void onRefreshStorage() {
        refreshStorageAndFileCount();
        Toast.makeText(this, "存储状态已刷新", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onModeChange(@NonNull FillMode mode) {
        fillConfig.setFillMode(mode);
        updateUiState();
    }

    @Override
    public void onRemainingBytesChange(long bytes) {
        fillConfig.setTargetRemainingBytes(bytes);
        updateUiState();
    }

    @Override
    public void onPercentageChange(int percentage) {
        fillConfig.setTargetPercentage(percentage);
        updateUiState();
    }

    @Override
    public void onSpecificSizeChange(long bytes) {
        fillConfig.setTargetSpecificSizeBytes(bytes);
        updateUiState();
    }

    @Override
    public void onContentTypeChange(@NonNull FillContentType type) {
        fillConfig.setContentType(type);
        // 若切换为多媒体内容(JPG/MP4)或联系人，自动关闭碎片化模式，避免规格冲突
        if (type != FillContentType.RAW_FILE && type != FillContentType.MIXED) {
            fillConfig.setFragmentationMode(false);
        }
        if ((type == FillContentType.CONTACTS || type == FillContentType.MIXED) && !checkHasContactsPermission()) {
            onRequestContactsPermission();
        }
        updateUiState();
    }

    @Override
    public void onRandomDataToggle(boolean isRandom) {
        fillConfig.setRandomData(isRandom);
        updateUiState();
    }

    @Override
    public void onChunkSizeChange(int sizeMb) {
        // 用户明确点击大分块大小，自动关闭碎片化模式，使该大分块配置立即且明确生效
        fillConfig.setChunkSizeBytes((long) sizeMb * 1024 * 1024);
        fillConfig.setFragmentationMode(false);
        updateUiState();
    }

    @Override
    public void onMediaGalleryToggle(boolean enable) {
        fillConfig.setImportToMediaGallery(enable);
        updateUiState();
    }

    @Override
    public void onConcurrencyChange(int threads) {
        fillConfig.setConcurrencyThreads(threads);
        updateUiState();
    }

    @Override
    public void onFragmentationToggle(boolean enabled) {
        fillConfig.setFragmentationMode(enabled);
        // 开启碎片化模式时，若当前为图片或视频，自动转为普通文件以适配微文件填充
        if (enabled && fillConfig.getContentType() != FillContentType.RAW_FILE && fillConfig.getContentType() != FillContentType.MIXED) {
            fillConfig.setContentType(FillContentType.RAW_FILE);
        }
        updateUiState();
    }

    @Override
    public void onFragmentSizeLevelChange(com.mobiiot.writerom.model.FragmentSizeLevel level) {
        fillConfig.setFragmentSizeLevel(level);
        updateUiState();
    }

    @Override
    public void onPunchHolesToggle(boolean enabled) {
        fillConfig.setPunchHolesEnabled(enabled);
        updateUiState();
    }

    @Override
    public void onStartTask() {
        if (storageService == null) {
            Toast.makeText(this, "后台服务未连接，正在重试...", Toast.LENGTH_SHORT).show();
            return;
        }

        if ((fillConfig.getContentType() == FillContentType.CONTACTS || fillConfig.getContentType() == FillContentType.MIXED)
                && !checkHasContactsPermission()) {
            Toast.makeText(this, "填充联系人需要通讯录权限，请先授予权限", Toast.LENGTH_LONG).show();
            onRequestContactsPermission();
            return;
        }

        currentStorage = StorageHelper.getStorageInfo(this);
        long targetBytes = fillConfig.calculateTargetFillBytes(currentStorage);
        if (targetBytes <= 0) {
            Toast.makeText(this, "当前已达到或超出设定目标，无需填充", Toast.LENGTH_LONG).show();
            return;
        }

        statusMessage = "任务启动中...";
        storageService.startFill(fillConfig, new FillEngine.OnProgressListener() {
            @Override
            public void onProgressUpdate(@NonNull FillProgress progress, @NonNull StorageInfo storageInfo) {
                runOnUiThread(() -> {
                    currentProgress = progress;
                    currentStorage = storageInfo;
                    updateUiState();
                });
            }

            @Override
            public void onStatusChanged(@NonNull TaskStatus status, @Nullable String message) {
                runOnUiThread(() -> {
                    statusMessage = message != null ? message : status.getLabel();
                    currentProgress = currentProgress.withStatus(status);
                    refreshStorageAndFileCount();
                    updateUiState();
                    if (status == TaskStatus.COMPLETED) {
                        Toast.makeText(MainActivity.this, "ROM 填充完成: " + statusMessage, Toast.LENGTH_LONG).show();
                    } else if (status == TaskStatus.ERROR) {
                        Toast.makeText(MainActivity.this, "ROM 填充异常: " + statusMessage, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
        updateUiState();
    }

    @Override
    public void onPauseTask() {
        if (storageService != null) {
            storageService.pauseFill();
            statusMessage = "任务已暂停";
            currentProgress = currentProgress.withStatus(TaskStatus.PAUSED);
            updateUiState();
        }
    }

    @Override
    public void onResumeTask() {
        if (storageService != null) {
            storageService.resumeFill();
            statusMessage = "任务已继续";
            currentProgress = currentProgress.withStatus(TaskStatus.RUNNING);
            updateUiState();
        }
    }

    @Override
    public void onStopTask() {
        if (storageService != null) {
            storageService.stopFill();
            statusMessage = "任务正在停止...";
            currentProgress = currentProgress.withStatus(TaskStatus.STOPPED);
            updateUiState();
        }
    }

    @Override
    public void onCleanAll() {
        isCleaning = true;
        cleaningDeletedCount = 0;
        cleaningReclaimedBytes = 0;
        statusMessage = "正在清理释放所有填充数据...";
        updateUiState();

        cleanupManager.cleanAllAsync(new CleanupManager.CleanupCallback() {
            @Override
            public void onCleanupStarted() {
                runOnUiThread(() -> {
                    isCleaning = true;
                    cleaningDeletedCount = 0;
                    cleaningReclaimedBytes = 0;
                    updateUiState();
                });
            }

            @Override
            public void onCleanupProgress(int deletedCount, long bytesReclaimed) {
                runOnUiThread(() -> {
                    cleaningDeletedCount = deletedCount;
                    cleaningReclaimedBytes = bytesReclaimed;
                    statusMessage = String.format("已清理 %d 个文件，释放 %s",
                            deletedCount, StorageHelper.formatSize(bytesReclaimed));
                    updateUiState();
                });
            }

            @Override
            public void onCleanupCompleted(int totalDeleted, long totalBytesReclaimed) {
                runOnUiThread(() -> {
                    isCleaning = false;
                    cleaningDeletedCount = totalDeleted;
                    cleaningReclaimedBytes = totalBytesReclaimed;
                    statusMessage = String.format("清理完成！共删除 %d 个文件，成功释放 %s 空间",
                            totalDeleted, StorageHelper.formatSize(totalBytesReclaimed));
                    currentProgress = FillProgress.createIdle();
                    refreshStorageAndFileCount();
                    Toast.makeText(MainActivity.this, statusMessage, Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onCleanupStopped(int totalDeleted, long totalBytesReclaimed) {
                runOnUiThread(() -> {
                    isCleaning = false;
                    cleaningDeletedCount = totalDeleted;
                    cleaningReclaimedBytes = totalBytesReclaimed;
                    statusMessage = String.format("已停止清理！共删除 %d 个文件，已释放 %s 空间",
                            totalDeleted, StorageHelper.formatSize(totalBytesReclaimed));
                    refreshStorageAndFileCount();
                    Toast.makeText(MainActivity.this, statusMessage, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    @Override
    public void onStopClean() {
        if (cleanupManager != null && isCleaning) {
            statusMessage = "正在停止清理...";
            updateUiState();
            cleanupManager.stopCleanup();
        }
    }

    @Override
    public void onRequestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, REQUEST_CODE_ALL_FILES);
            } catch (Exception e) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivityForResult(intent, REQUEST_CODE_ALL_FILES);
            }
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_ALL_FILES);
        }
    }

    @Override
    public void onFloatingHudToggle(boolean enabled) {
        FloatingHudManager hudManager = FloatingHudManager.getInstance(this);
        if (enabled) {
            if (!hudManager.canDrawOverlays()) {
                onRequestOverlayPermission();
            } else {
                hudManager.showHud();
            }
        } else {
            hudManager.hideHud();
        }
        updateUiState();
    }

    @Override
    public void onRequestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, REQUEST_CODE_OVERLAY);
            } catch (Exception e) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                startActivityForResult(intent, REQUEST_CODE_OVERLAY);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_OVERLAY) {
            FloatingHudManager hudManager = FloatingHudManager.getInstance(this);
            if (hudManager.canDrawOverlays()) {
                hudManager.showHud();
                Toast.makeText(this, "已获得悬浮窗权限，已开启微型仪表盘", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "未授予悬浮窗权限", Toast.LENGTH_SHORT).show();
            }
            updateUiState();
        } else if (requestCode == REQUEST_CODE_ALL_FILES) {
            refreshStorageAndFileCount();
        }
    }
}
