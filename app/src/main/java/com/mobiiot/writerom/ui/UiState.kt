package com.mobiiot.writerom.ui

import androidx.compose.runtime.Immutable
import com.mobiiot.writerom.model.FillContentType
import com.mobiiot.writerom.model.FillMode
import com.mobiiot.writerom.model.FragmentSizeLevel
import com.mobiiot.writerom.model.TaskStatus

@Immutable
data class StorageUiState @JvmOverloads constructor(
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
    val targetRemainingBytes: Long = 0L,
    val targetPercentage: Int = 100,
    val targetSpecificSizeBytes: Long = 10L * 1024 * 1024 * 1024,
    val contentType: FillContentType = FillContentType.RAW_FILE,
    val isRandomData: Boolean = false,
    val chunkSizeMb: Int = 500,
    val importToMediaGallery: Boolean = false,

    // 高级压测配置 (v1.2)
    val concurrencyThreads: Int = 1,
    val isFragmentationMode: Boolean = false,
    val fragmentSizeLevel: FragmentSizeLevel = FragmentSizeLevel.TINY_4K_16K,
    val isPunchHolesEnabled: Boolean = false,
    val activeThreads: Int = 1,

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
    val existingFillerFilesCount: Int = 0,
    val cleaningDeletedCount: Int = 0,
    val cleaningReclaimedBytes: Long = 0L,
    val formattedCleaningReclaimed: String = "0 B",

    // v1.3: 性能衰减折线图 (Canvas) 与 SLC 缓存感知
    val speedHistory: List<Float> = emptyList(),
    val peakSpeedMb: Float = 0f,
    val avgSpeedMb: Float = 0f,
    val isSlcExhausted: Boolean = false,

    // v1.3: 硬件温度与寿命感知
    val deviceTemperature: Float = 0f,
    val thermalStatusLevel: Int = 0,
    val thermalStatusLabel: String = "正常",
    val lifetimeBytesWritten: Long = 0L,
    val formattedLifetimeBytes: String = "0 B",
    val estimatedPeCycles: Double = 0.0,

    // v1.4: 悬浮微型仪表盘 (Floating HUD)
    val isFloatingHudEnabled: Boolean = false,
    val hasOverlayPermission: Boolean = false
) {
    val effectiveWriteSpec: String
        get() = when {
            contentType == FillContentType.IMAGE -> "合规 JPEG 图片"
            contentType == FillContentType.VIDEO -> "标准 MP4 视频"
            isFragmentationMode -> when (fragmentSizeLevel) {
                FragmentSizeLevel.TINY_4K_16K -> "4K~16K 碎片"
                FragmentSizeLevel.SMALL_32K_128K -> "32K~128K 碎片"
                FragmentSizeLevel.MIXED_4K_256K -> "4K~256K 碎片"
            }
            else -> "${chunkSizeMb}MB 分块" + if (contentType == FillContentType.MIXED) " (混合)" else ""
        }
}
