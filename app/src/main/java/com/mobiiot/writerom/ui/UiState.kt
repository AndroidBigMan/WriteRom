package com.mobiiot.writerom.ui

import androidx.compose.runtime.Immutable
import com.mobiiot.writerom.model.FillContentType
import com.mobiiot.writerom.model.FillMode
import com.mobiiot.writerom.model.TaskStatus

@Immutable
data class StorageUiState(
    // 存储感知
    val totalBytes: Long = 0L,
    val freeBytes: Long = 0L,
    val usedBytes: Long = 0L,
    val percentUsed: Double = 0.0,
    val formattedTotal: String = "0 B",
    val formattedFree: String = "0 B",
    val formattedUsed: String = "0 B",

    // 填充配置
    val fillMode: FillMode = FillMode.TARGET_REMAINING_SPACE,
    val targetRemainingBytes: Long = 0L, // 用户选定默认不保留
    val targetPercentage: Int = 100,
    val targetSpecificSizeBytes: Long = 10L * 1024 * 1024 * 1024,
    val contentType: FillContentType = FillContentType.RAW_FILE,
    val isRandomData: Boolean = false,
    val chunkSizeMb: Int = 500,
    val importToMediaGallery: Boolean = false,

    // 预估填充体积
    val estimatedFillBytes: Long = 0L,
    val formattedEstimatedFill: String = "0 B",

    // 运行状态与进度
    val taskStatus: TaskStatus = TaskStatus.IDLE,
    val bytesWritten: Long = 0L,
    val targetTotalBytes: Long = 0L,
    val progressPercent: Float = 0f,
    val speedBytesPerSec: Double = 0.0,
    val formattedSpeed: String = "0 KB/s",
    val formattedWritten: String = "0 B",
    val formattedTarget: String = "0 B",
    val formattedElapsed: String = "00:00",
    val formattedEta: String = "--:--",
    val filesCreatedCount: Int = 0,
    val currentFileName: String = "",
    val statusMessage: String = "",

    // 权限与清理
    val hasStoragePermission: Boolean = true,
    val hasNotificationPermission: Boolean = true,
    val isCleaning: Boolean = false,
    val existingFillerFilesCount: Int = 0
)
