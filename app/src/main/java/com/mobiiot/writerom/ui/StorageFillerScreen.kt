package com.mobiiot.writerom.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobiiot.writerom.model.FillContentType
import com.mobiiot.writerom.model.FillMode
import com.mobiiot.writerom.model.FragmentSizeLevel
import com.mobiiot.writerom.model.TaskStatus
import com.mobiiot.writerom.ui.components.*
import com.mobiiot.writerom.ui.theme.DarkBackground
import com.mobiiot.writerom.ui.theme.DarkGlassSurface
import com.mobiiot.writerom.ui.theme.DarkSurface
import com.mobiiot.writerom.ui.theme.ElectricBlue
import com.mobiiot.writerom.ui.theme.NeonAmber
import com.mobiiot.writerom.ui.theme.NeonCyan
import com.mobiiot.writerom.ui.theme.NeonEmerald
import com.mobiiot.writerom.ui.theme.NeonRose
import com.mobiiot.writerom.ui.theme.bounceClickable
import com.mobiiot.writerom.ui.theme.glassBorder

/**
 * ROM 存储填充主界面 (StorageFillerScreen)
 * Direction C: Modern Neon Glassmorphic Bento · 4大独立Tab分页架构
 */
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
    onFragmentSizeLevelChange: (FragmentSizeLevel) -> Unit,
    onPunchHolesToggle: (Boolean) -> Unit,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onCleanAll: () -> Unit,
    onStopClean: () -> Unit,
    onRequestStoragePermission: () -> Unit,
    onRequestContactsPermission: () -> Unit,
    onFloatingHudToggle: (Boolean) -> Unit,
    onRequestOverlayPermission: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

    // 当前选中的分页 Tab 索引 (0: 全景监控, 1: 性能示波, 2: 填充策略, 3: 空间工具)
    var selectedTab by remember { mutableStateOf(0) }

    val tabTitles = listOf("存储全景监控", "写入性能示波", "填充压测策略", "空间管理工具")
    val tabSubtitles = listOf("TELEMETRY", "OSCILLOGRAM", "STRATEGY", "UTILITIES")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(NeonCyan)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = tabTitles[selectedTab],
                                fontSize = 18.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "TAB ${selectedTab + 1} · ${tabSubtitles[selectedTab]}",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = NeonCyan.copy(alpha = 0.85f),
                                letterSpacing = 0.8.sp
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = onRefreshStorage,
                        modifier = Modifier.bounceClickable(onClick = onRefreshStorage)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = NeonCyan,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground.copy(alpha = 0.95f)
                )
            )
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
                .drawBehind {
                    // Ambient Neon Cyan aura at top-right
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(NeonCyan.copy(alpha = 0.07f), Color.Transparent),
                            center = Offset(size.width * 0.9f, 0f),
                            radius = size.width * 0.75f
                        )
                    )
                    // Ambient Neon Rose aura at bottom-left
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(NeonRose.copy(alpha = 0.05f), Color.Transparent),
                            center = Offset(size.width * 0.1f, size.height * 0.85f),
                            radius = size.width * 0.85f
                        )
                    )
                }
                .padding(innerPadding)
        ) {
            // ==========================================
            // 页面内容区域 (带独立滚动记忆与底部防遮挡边距)
            // ==========================================
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = if (isTablet) 20.dp else 16.dp, vertical = 12.dp)
            ) {
                when (selectedTab) {
                    // ================= TAB 0: 全景监控 (Monitor / Dashboard) =================
                    0 -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(bottom = 160.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // 权限提示横幅
                            PermissionBanners(
                                uiState = uiState,
                                onRequestStoragePermission = onRequestStoragePermission,
                                onRequestContactsPermission = onRequestContactsPermission
                            )

                            // 1. 全盘存储总览 Hero Bento
                            StorageOverviewCard(uiState = uiState)

                            // 2. 实时 4 宫格写入监控看板
                            LiveDashboardCard(uiState = uiState)

                            // 3. 闪存硬件健康与芯片温控感知
                            HardwareHealthCard(uiState = uiState)
                        }
                    }

                    // ================= TAB 1: 性能示波 (Performance / Oscillogram) =================
                    1 -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(bottom = 160.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // 1. 实时写入速率衰减与示波器折线图
                            PerformanceChartCard(uiState = uiState)

                            // 2. SLC 缓存衰减与直写深度诊断 Bento
                            SlcLifecycleDiagnosticCard(uiState = uiState)
                        }
                    }

                    // ================= TAB 2: 填充策略 (Strategy / Config) =================
                    2 -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(bottom = 160.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // 1. 阈值与容量目标设置
                            ThresholdConfigCard(
                                uiState = uiState,
                                onModeChange = onModeChange,
                                onRemainingBytesChange = onRemainingBytesChange,
                                onPercentageChange = onPercentageChange,
                                onSpecificSizeChange = onSpecificSizeChange
                            )

                            // 2. 填充内容类型与介质规格
                            ContentTypeCard(
                                uiState = uiState,
                                onContentTypeChange = onContentTypeChange,
                                onRandomDataToggle = onRandomDataToggle,
                                onChunkSizeChange = onChunkSizeChange,
                                onFragmentationToggle = onFragmentationToggle,
                                onMediaGalleryToggle = onMediaGalleryToggle
                            )

                            // 3. 多线程并发与打孔高级压测
                            AdvancedStressCard(
                                uiState = uiState,
                                onConcurrencyChange = onConcurrencyChange,
                                onFragmentationToggle = onFragmentationToggle,
                                onFragmentSizeLevelChange = onFragmentSizeLevelChange,
                                onPunchHolesToggle = onPunchHolesToggle
                            )
                        }
                    }

                    // ================= TAB 3: 空间工具 (Tools & Cleanup) =================
                    3 -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(bottom = 160.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // 1. 空间全量清理与释放卡片
                            ControlActionsCard(
                                uiState = uiState,
                                onStart = onStart,
                                onPause = onPause,
                                onResume = onResume,
                                onStop = onStop,
                                onCleanAll = onCleanAll,
                                onStopClean = onStopClean
                            )

                            // 2. 全局微型悬浮仪表盘控制
                            FloatingHudCard(
                                uiState = uiState,
                                onToggle = onFloatingHudToggle,
                                onRequestPermission = onRequestOverlayPermission
                            )

                            // 3. 底层文件系统与挂载点诊断详情卡片
                            FileSystemDiagnosticsCard()
                        }
                    }
                }
            }

            // ==========================================
            // 常驻悬浮控制岛 (Sticky Island)
            // 悬浮在底部导航栏之上，跨所有 Tab 随时操作
            // ==========================================
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
                    .padding(bottom = 74.dp)
                    .padding(horizontal = if (isTablet) 32.dp else 12.dp)
            )

            // ==========================================
            // 现代毛玻璃底部导航栏 (Bottom Navigation Bar)
            // ==========================================
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(64.dp)
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            listOf(Color.White.copy(alpha = 0.12f), Color.Transparent)
                        ),
                        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                    ),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                color = DarkSurface.copy(alpha = 0.96f),
                tonalElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val tabLabels = listOf("全景监控", "性能示波", "填充策略", "空间工具")
                    tabLabels.forEachIndexed { index, label ->
                        val isSelected = selectedTab == index
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .bounceClickable(scaleDown = 0.92f) { selectedTab = index }
                                .padding(vertical = 4.dp)
                        ) {
                            TabVectorIcon(
                                tabIndex = index,
                                tint = if (isSelected) NeonCyan else Color.White.copy(alpha = 0.45f)
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                color = if (isSelected) NeonCyan else Color.White.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 自绘轻量级霓虹几何矢量导航图标 (无外部图标库依赖)
 */
@Composable
private fun TabVectorIcon(
    tabIndex: Int,
    tint: Color,
    modifier: Modifier = Modifier.size(20.dp)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        when (tabIndex) {
            0 -> {
                // Dashboard Gauge 柱状条
                val barW = w * 0.22f
                val r = CornerRadius(barW / 2, barW / 2)
                drawRoundRect(color = tint, topLeft = Offset(w * 0.1f, h * 0.48f), size = Size(barW, h * 0.52f), cornerRadius = r)
                drawRoundRect(color = tint, topLeft = Offset(w * 0.39f, h * 0.18f), size = Size(barW, h * 0.82f), cornerRadius = r)
                drawRoundRect(color = tint, topLeft = Offset(w * 0.68f, h * 0.35f), size = Size(barW, h * 0.65f), cornerRadius = r)
            }
            1 -> {
                // Oscillogram 脉冲波形
                val p = Path().apply {
                    moveTo(0f, h * 0.62f)
                    lineTo(w * 0.25f, h * 0.62f)
                    lineTo(w * 0.42f, h * 0.16f)
                    lineTo(w * 0.62f, h * 0.86f)
                    lineTo(w * 0.78f, h * 0.5f)
                    lineTo(w, h * 0.5f)
                }
                drawPath(p, color = tint, style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            2 -> {
                // Strategy 滑块微调
                drawLine(color = tint, start = Offset(0f, h * 0.32f), end = Offset(w, h * 0.32f), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
                drawCircle(color = tint, radius = 3.2.dp.toPx(), center = Offset(w * 0.32f, h * 0.32f))
                drawLine(color = tint, start = Offset(0f, h * 0.68f), end = Offset(w, h * 0.68f), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
                drawCircle(color = tint, radius = 3.2.dp.toPx(), center = Offset(w * 0.7f, h * 0.68f))
            }
            3 -> {
                // Tools 运维工具箱
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.15f, h * 0.24f),
                    size = Size(w * 0.7f, h * 0.64f),
                    cornerRadius = CornerRadius(3.5.dp.toPx(), 3.5.dp.toPx()),
                    style = Stroke(width = 2.dp.toPx())
                )
                drawLine(color = tint, start = Offset(w * 0.36f, h * 0.24f), end = Offset(w * 0.36f, h * 0.14f), strokeWidth = 1.8.dp.toPx(), cap = StrokeCap.Round)
                drawLine(color = tint, start = Offset(w * 0.64f, h * 0.24f), end = Offset(w * 0.64f, h * 0.14f), strokeWidth = 1.8.dp.toPx(), cap = StrokeCap.Round)
                drawLine(color = tint, start = Offset(w * 0.36f, h * 0.14f), end = Offset(w * 0.64f, h * 0.14f), strokeWidth = 1.8.dp.toPx(), cap = StrokeCap.Round)
            }
        }
    }
}

/**
 * 权限引导横幅
 */
@Composable
private fun PermissionBanners(
    uiState: StorageUiState,
    onRequestStoragePermission: () -> Unit,
    onRequestContactsPermission: () -> Unit
) {
    if (!uiState.hasStoragePermission) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .glassBorder(18.dp),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = DarkGlassSurface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
                    tint = NeonAmber,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "需要所有文件管理权限",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "需要该权限以直接在设备根目录极速填充与清理。",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Button(
                    onClick = onRequestStoragePermission,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonAmber),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.bounceClickable(onClick = onRequestStoragePermission)
                ) {
                    Text("去授权", fontSize = 13.sp, color = Color(0xFF040D1A), fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if ((uiState.contentType == FillContentType.CONTACTS || uiState.contentType == FillContentType.MIXED) && !uiState.hasContactsPermission) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .glassBorder(18.dp),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = DarkGlassSurface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
                    tint = NeonAmber,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "需要通讯录写入权限",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "当前选择的模式包含联系人填充，需要通讯录写入权限以进行压力测试。",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Button(
                    onClick = onRequestContactsPermission,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonAmber),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.bounceClickable(onClick = onRequestContactsPermission)
                ) {
                    Text("去授权", fontSize = 13.sp, color = Color(0xFF040D1A), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * 性能示波专属：SLC 缓存生命周期深度诊断 Bento
 */
@Composable
private fun SlcLifecycleDiagnosticCard(uiState: StorageUiState) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .glassBorder(24.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = DarkGlassSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (uiState.isSlcExhausted) NeonAmber else NeonEmerald)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "SLC 缓存生命周期阶段诊断",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "连续突发大文件写入时，闪存颗粒初期以拟 SLC 模式（单 Cell 1 bit）极速爆发；当缓存耗尽后，主控被迫执行垃圾回收（GC）并回退至 TLC 原生直写模式。",
                fontSize = 13.sp,
                lineHeight = 20.sp,
                color = Color.White.copy(alpha = 0.75f)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                StageRow(
                    stage = "阶段 1: SLC 模拟爆发区",
                    speedRange = "740 ~ 820 MB/s",
                    isActive = !uiState.isSlcExhausted && uiState.taskStatus == TaskStatus.RUNNING,
                    activeColor = NeonCyan
                )
                StageRow(
                    stage = "阶段 2: TLC 原生直写区",
                    speedRange = "220 ~ 260 MB/s",
                    isActive = uiState.isSlcExhausted,
                    activeColor = NeonAmber
                )
                StageRow(
                    stage = "阶段 3: GC 折叠回收区",
                    speedRange = "90 ~ 140 MB/s",
                    isActive = false,
                    activeColor = NeonRose
                )
            }
        }
    }
}

@Composable
private fun StageRow(stage: String, speedRange: String, isActive: Boolean, activeColor: Color) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isActive) activeColor.copy(alpha = 0.12f) else DarkSurface.copy(alpha = 0.6f),
        border = if (isActive) androidx.compose.foundation.BorderStroke(1.dp, activeColor.copy(alpha = 0.4f)) else null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 11.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stage,
                fontSize = 13.5.sp,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                color = if (isActive) activeColor else Color.White.copy(alpha = 0.7f)
            )
            Text(
                text = speedRange,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                color = if (isActive) activeColor else Color.White.copy(alpha = 0.5f)
            )
        }
    }
}

/**
 * 空间工具专属：底层文件系统与挂载点诊断详情卡片
 */
@Composable
private fun FileSystemDiagnosticsCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .glassBorder(24.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = DarkGlassSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp)
        ) {
            Text(
                text = "文件系统与挂载点诊断",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(14.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DiagnosticRow(label = "主存储挂载路径", value = "/storage/emulated/0")
                DiagnosticRow(label = "底层文件系统类型", value = "F2FS (Flash-Friendly)", valueColor = NeonCyan)
                DiagnosticRow(label = "扇区分配块尺寸", value = "4096 Bytes (4KB)")
                DiagnosticRow(label = "TRIM 与损耗均衡", value = "AUTO ENABLED", valueColor = NeonEmerald)
            }
        }
    }
}

@Composable
private fun DiagnosticRow(label: String, value: String, valueColor: Color = Color.White.copy(alpha = 0.85f)) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = DarkSurface.copy(alpha = 0.6f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 11.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, fontSize = 13.sp, color = Color.White.copy(alpha = 0.6f))
            Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = valueColor)
        }
    }
}
