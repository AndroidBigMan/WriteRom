package com.mobiiot.writerom.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobiiot.writerom.model.FillContentType
import com.mobiiot.writerom.model.FillMode
import com.mobiiot.writerom.model.TaskStatus
import com.mobiiot.writerom.ui.components.*
import com.mobiiot.writerom.ui.theme.WarningAmber
import com.mobiiot.writerom.ui.theme.bounceClickable
import com.mobiiot.writerom.ui.theme.glassBorder

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
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600
    val isRunning = uiState.taskStatus == TaskStatus.RUNNING || uiState.taskStatus == TaskStatus.PAUSED
    var phoneConfigExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "ROM 存储填充助手",
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(
                        onClick = onRefreshStorage,
                        modifier = Modifier.bounceClickable(onClick = onRefreshStorage)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = if (isTablet) 20.dp else 16.dp, vertical = 14.dp)
                    .padding(bottom = if (isTablet) 110.dp else 96.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // 权限提示横幅 (若未获得权限，精简为一行清晰说明)
                if (!uiState.hasStoragePermission) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassBorder(16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = WarningAmber.copy(alpha = 0.15f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Warning",
                                tint = WarningAmber,
                                modifier = Modifier.size(30.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "需要所有文件管理权限",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "需要该权限以直接在设备根目录极速填充与清理。",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Button(
                                onClick = onRequestStoragePermission,
                                colors = ButtonDefaults.buttonColors(containerColor = WarningAmber),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.bounceClickable(onClick = onRequestStoragePermission)
                            ) {
                                Text("去授权", fontSize = 13.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                if (isTablet) {
                    // ==========================================
                    // 平板 / 大屏 (screenWidthDp >= 600) 双栏 Bento 自适应布局
                    // ==========================================
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        // 左栏：遥测与状态 (40% ~ 50% 视觉锚点)
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(18.dp)
                        ) {
                            StorageOverviewCard(uiState = uiState)
                            LiveDashboardCard(uiState = uiState)
                            PerformanceChartCard(uiState = uiState)
                            HardwareHealthCard(uiState = uiState)
                        }

                        // 右栏：控制与参数配置
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(18.dp)
                        ) {
                            FloatingHudCard(
                                uiState = uiState,
                                onToggle = onFloatingHudToggle,
                                onRequestPermission = onRequestOverlayPermission
                            )
                            ThresholdConfigCard(
                                uiState = uiState,
                                onModeChange = onModeChange,
                                onRemainingBytesChange = onRemainingBytesChange,
                                onPercentageChange = onPercentageChange,
                                onSpecificSizeChange = onSpecificSizeChange
                            )
                            ContentTypeCard(
                                uiState = uiState,
                                onContentTypeChange = onContentTypeChange,
                                onRandomDataToggle = onRandomDataToggle,
                                onChunkSizeChange = onChunkSizeChange,
                                onFragmentationToggle = onFragmentationToggle,
                                onMediaGalleryToggle = onMediaGalleryToggle
                            )
                            AdvancedStressCard(
                                uiState = uiState,
                                onConcurrencyChange = onConcurrencyChange,
                                onFragmentationToggle = onFragmentationToggle,
                                onFragmentSizeLevelChange = onFragmentSizeLevelChange,
                                onPunchHolesToggle = onPunchHolesToggle
                            )
                            ControlActionsCard(
                                uiState = uiState,
                                onStart = onStart,
                                onPause = onPause,
                                onResume = onResume,
                                onStop = onStop,
                                onCleanAll = onCleanAll,
                                onStopClean = onStopClean
                            )
                        }
                    }
                } else {
                    // ==========================================
                    // 手机竖屏 (单栏流式 + 运行时智能折叠 Context Folding)
                    // ==========================================
                    // 1. 存储总览与遥测 Hero
                    StorageOverviewCard(uiState = uiState)

                    // 2. 实时监控看板 (执行时动态展开)
                    LiveDashboardCard(uiState = uiState)

                    // 3. 实时写入性能衰减折线图
                    PerformanceChartCard(uiState = uiState)

                    // 4. 闪存健康与芯片温控感知
                    HardwareHealthCard(uiState = uiState)

                    // 5. 全局悬浮微型仪表盘
                    FloatingHudCard(
                        uiState = uiState,
                        onToggle = onFloatingHudToggle,
                        onRequestPermission = onRequestOverlayPermission
                    )

                    // 6. 策略与参数配置区域（任务进行中支持智能折叠）
                    if (isRunning) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .glassBorder(16.dp)
                                .bounceClickable { phoneConfigExpanded = !phoneConfigExpanded }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "填充策略与高级配置",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                    ) {
                                        Text(
                                            text = if (phoneConfigExpanded) "已展开" else "执行中已折叠",
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = if (phoneConfigExpanded) "收起 ▲" else "展开查看 ▼",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        AnimatedVisibility(
                            visible = phoneConfigExpanded,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                                ThresholdConfigCard(
                                    uiState = uiState,
                                    onModeChange = onModeChange,
                                    onRemainingBytesChange = onRemainingBytesChange,
                                    onPercentageChange = onPercentageChange,
                                    onSpecificSizeChange = onSpecificSizeChange
                                )
                                ContentTypeCard(
                                    uiState = uiState,
                                    onContentTypeChange = onContentTypeChange,
                                    onRandomDataToggle = onRandomDataToggle,
                                    onChunkSizeChange = onChunkSizeChange,
                                    onFragmentationToggle = onFragmentationToggle,
                                    onMediaGalleryToggle = onMediaGalleryToggle
                                )
                                AdvancedStressCard(
                                    uiState = uiState,
                                    onConcurrencyChange = onConcurrencyChange,
                                    onFragmentationToggle = onFragmentationToggle,
                                    onFragmentSizeLevelChange = onFragmentSizeLevelChange,
                                    onPunchHolesToggle = onPunchHolesToggle
                                )
                            }
                        }
                    } else {
                        // 空闲/就绪状态下，直接展示完整配置项供用户调整
                        ThresholdConfigCard(
                            uiState = uiState,
                            onModeChange = onModeChange,
                            onRemainingBytesChange = onRemainingBytesChange,
                            onPercentageChange = onPercentageChange,
                            onSpecificSizeChange = onSpecificSizeChange
                        )
                        ContentTypeCard(
                            uiState = uiState,
                            onContentTypeChange = onContentTypeChange,
                            onRandomDataToggle = onRandomDataToggle,
                            onChunkSizeChange = onChunkSizeChange,
                            onFragmentationToggle = onFragmentationToggle,
                            onMediaGalleryToggle = onMediaGalleryToggle
                        )
                        AdvancedStressCard(
                            uiState = uiState,
                            onConcurrencyChange = onConcurrencyChange,
                            onFragmentationToggle = onFragmentationToggle,
                            onFragmentSizeLevelChange = onFragmentSizeLevelChange,
                            onPunchHolesToggle = onPunchHolesToggle
                        )
                    }

                    // 7. 操作控制卡片（开始、暂停、停止、一键清理）
                    ControlActionsCard(
                        uiState = uiState,
                        onStart = onStart,
                        onPause = onPause,
                        onResume = onResume,
                        onStop = onStop,
                        onCleanAll = onCleanAll,
                        onStopClean = onStopClean
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
            }

            // 常驻底部半透明悬浮控制岛 (Sticky Bottom Action Bar)
            StickyBottomActionBar(
                uiState = uiState,
                onStart = onStart,
                onPause = onPause,
                onResume = onResume,
                onStop = onStop,
                onCleanAll = onCleanAll,
                onStopClean = onStopClean,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = if (isTablet) 32.dp else 14.dp, vertical = 12.dp)
            )
        }
    }
}
