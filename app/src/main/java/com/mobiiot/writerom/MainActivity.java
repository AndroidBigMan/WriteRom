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
import com.mobiiot.writerom.utils.StorageHelper;

public class MainActivity extends ComponentActivity implements UiBridge.UiCallbacks {

    private static final int REQUEST_CODE_NOTIF = 101;
    private static final int REQUEST_CODE_ALL_FILES = 102;

    private ComposeView composeView;
    private UiBridge.BridgeController bridgeController;

    private StorageFillerService storageService;
    private boolean isServiceBound = false;

    private FillConfig fillConfig;
    private StorageInfo currentStorage;
    private FillProgress currentProgress;
    private CleanupManager cleanupManager;

    private boolean isCleaning = false;
    private int existingFilesCount = 0;
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

        // 2. 初始化 Kotlin ComposeView 桥接
        bridgeController = new UiBridge.BridgeController(this);
        UiBridge.bindComposeView(composeView, bridgeController);

        // 3. 启动并绑定前台服务
        Intent serviceIntent = new Intent(this, StorageFillerService.class);
        startService(serviceIntent);
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);

        // 4. 权限与状态检查
        checkPermissions();
        refreshStorageAndFileCount();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshStorageAndFileCount();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
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
                        0, 0, 0, 0, "", null
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

        long estimatedFill = fillConfig.calculateTargetFillBytes(currentStorage);
        String formattedEst = StorageHelper.formatSize(estimatedFill);

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
                isCleaning,
                existingFilesCount
        );

        bridgeController.updateState(uiState);
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
        // 若切换为多媒体内容(JPG/MP4)，自动关闭碎片化模式，避免规格冲突
        if (type != FillContentType.RAW_FILE && type != FillContentType.MIXED) {
            fillConfig.setFragmentationMode(false);
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
        statusMessage = "正在清理释放所有填充数据...";
        updateUiState();

        cleanupManager.cleanAllAsync(new CleanupManager.CleanupCallback() {
            @Override
            public void onCleanupStarted() {
                runOnUiThread(() -> {
                    isCleaning = true;
                    updateUiState();
                });
            }

            @Override
            public void onCleanupProgress(int deletedCount, long bytesReclaimed) {
                runOnUiThread(() -> {
                    statusMessage = String.format("已清理 %d 个文件，释放 %s",
                            deletedCount, StorageHelper.formatSize(bytesReclaimed));
                    updateUiState();
                });
            }

            @Override
            public void onCleanupCompleted(int totalDeleted, long totalBytesReclaimed) {
                runOnUiThread(() -> {
                    isCleaning = false;
                    statusMessage = String.format("清理完成！共删除 %d 个文件，成功释放 %s 空间",
                            totalDeleted, StorageHelper.formatSize(totalBytesReclaimed));
                    currentProgress = FillProgress.createIdle();
                    refreshStorageAndFileCount();
                    Toast.makeText(MainActivity.this, statusMessage, Toast.LENGTH_LONG).show();
                });
            }
        });
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
}
