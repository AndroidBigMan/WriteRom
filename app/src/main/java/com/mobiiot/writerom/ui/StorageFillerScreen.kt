package com.mobiiot.writerom.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobiiot.writerom.model.FillContentType
import com.mobiiot.writerom.model.FillMode
import com.mobiiot.writerom.ui.components.*
import com.mobiiot.writerom.ui.theme.WarningAmber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageFillerScreen(
    uiState: StorageUiState,
    onRefreshStorage: () -> Unit,
    onModeChange: (FillMode) -> Unit,
    onRemainingBytesChange: (Long) -> Unit,
    onPercentageChange: (Int) -> Unit,
    onSpecificSizeChange: (Long) -> Unit,
    onContentTypeChange: (FillContentType) -> Unit,
    onRandomDataToggle: (Boolean) -> Unit,
    onChunkSizeChange: (Int) -> Unit,
    onMediaGalleryToggle: (Boolean) -> Unit,
    onConcurrencyChange: (Int) -> Unit,
    onFragmentationToggle: (Boolean) -> Unit,
    onFragmentSizeLevelChange: (com.mobiiot.writerom.model.FragmentSizeLevel) -> Unit,
    onPunchHolesToggle: (Boolean) -> Unit,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onCleanAll: () -> Unit,
    onStopClean: () -> Unit,
    onRequestStoragePermission: () -> Unit,
    onFloatingHudToggle: (Boolean) -> Unit,
    onRequestOverlayPermission: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "ROM 存储填充助手",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "容量压测 · 边缘测试 · 闪存填满",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onRefreshStorage) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 权限提示横幅
            if (!uiState.hasStoragePermission) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = WarningAmber.copy(alpha = 0.15f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Warning",
                            tint = WarningAmber,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "需要所有文件管理权限",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "授予权限以支持在根目录直接快速大批量填充与清理，若未授予将使用应用专属目录。",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = onRequestStoragePermission,
                            colors = ButtonDefaults.buttonColors(containerColor = WarningAmber),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("授权", fontSize = 12.sp, color = Color.Black)
                        }
                    }
                }
            }

            // 1. ROM 存储仪表盘卡片
            StorageOverviewCard(uiState = uiState)

            // 2. 实时写入监控卡片 (进行中/暂停/完成状态下展示)
            LiveDashboardCard(uiState = uiState)

            // 3. 实时写入性能衰减折线图卡片 (v1.3 新增)
            PerformanceChartCard(uiState = uiState)

            // 4. 闪存健康与芯片温控感知卡片 (v1.3 新增)
            HardwareHealthCard(uiState = uiState)

            // 5. 全局悬浮微型仪表盘卡片 (v1.4 新增)
            com.mobiiot.writerom.ui.components.FloatingHudCard(
                uiState = uiState,
                onToggle = onFloatingHudToggle,
                onRequestPermission = onRequestOverlayPermission
            )

            // 6. 填充阈值与策略设置卡片
            ThresholdConfigCard(
                uiState = uiState,
                onModeChange = onModeChange,
                onRemainingBytesChange = onRemainingBytesChange,
                onPercentageChange = onPercentageChange,
                onSpecificSizeChange = onSpecificSizeChange
            )

            // 7. 填充内容类型设置卡片
            ContentTypeCard(
                uiState = uiState,
                onContentTypeChange = onContentTypeChange,
                onRandomDataToggle = onRandomDataToggle,
                onChunkSizeChange = onChunkSizeChange,
                onFragmentationToggle = onFragmentationToggle,
                onMediaGalleryToggle = onMediaGalleryToggle
            )

            // 8. 高级压测与并发设置卡片 (v1.2)
            AdvancedStressCard(
                uiState = uiState,
                onConcurrencyChange = onConcurrencyChange,
                onFragmentationToggle = onFragmentationToggle,
                onFragmentSizeLevelChange = onFragmentSizeLevelChange,
                onPunchHolesToggle = onPunchHolesToggle
            )

            // 9. 操作控制卡片（开始、暂停、停止、一键清理）
            ControlActionsCard(
                uiState = uiState,
                onStart = onStart,
                onPause = onPause,
                onResume = onResume,
                onStop = onStop,
                onCleanAll = onCleanAll,
                onStopClean = onStopClean
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
