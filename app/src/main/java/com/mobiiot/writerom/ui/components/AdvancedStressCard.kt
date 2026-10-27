package com.mobiiot.writerom.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobiiot.writerom.model.FillContentType
import com.mobiiot.writerom.model.FragmentSizeLevel
import com.mobiiot.writerom.model.TaskStatus
import com.mobiiot.writerom.ui.StorageUiState
import com.mobiiot.writerom.ui.theme.DarkGlassSurface
import com.mobiiot.writerom.ui.theme.DarkSurface
import com.mobiiot.writerom.ui.theme.NeonAmber
import com.mobiiot.writerom.ui.theme.NeonCyan
import com.mobiiot.writerom.ui.theme.NeonPurple
import com.mobiiot.writerom.ui.theme.bounceClickable
import com.mobiiot.writerom.ui.theme.glassBorder

/**
 * 高级压测与并发设置卡片
 * Direction C: Modern Neon Glassmorphic Bento
 */
@Composable
fun AdvancedStressCard(
    uiState: StorageUiState,
    onConcurrencyChange: (Int) -> Unit,
    onFragmentationToggle: (Boolean) -> Unit,
    onFragmentSizeLevelChange: (FragmentSizeLevel) -> Unit,
    onPunchHolesToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val isRunning = uiState.taskStatus == TaskStatus.RUNNING || uiState.taskStatus == TaskStatus.PAUSED

    Card(
        modifier = modifier
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
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(NeonPurple.copy(alpha = 0.15f))
                        .border(1.dp, NeonPurple.copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Build,
                        contentDescription = "Advanced",
                        tint = NeonPurple,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "高级压测与并发设置",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = NeonPurple.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "并发与碎片",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonPurple,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 1. 并发线程数设置 (1 ~ 8)
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "多线程并发写入",
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Text(
                        text = "${uiState.concurrencyThreads} 线程并发",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = NeonCyan
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf(1, 2, 4, 8).forEach { t ->
                        val isSelected = uiState.concurrencyThreads == t
                        FilterChip(
                            selected = isSelected,
                            onClick = { onConcurrencyChange(t) },
                            label = {
                                Text(
                                    "${t}T",
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            enabled = !isRunning,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeonCyan.copy(alpha = 0.20f),
                                selectedLabelColor = NeonCyan,
                                containerColor = DarkSurface,
                                labelColor = Color.White.copy(alpha = 0.7f)
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = !isRunning,
                                selected = isSelected,
                                borderColor = if (isSelected) NeonCyan.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.1f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .bounceClickable(enabled = !isRunning) { onConcurrencyChange(t) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
            Spacer(modifier = Modifier.height(14.dp))

            // 2. 写入模式切换：常规顺序大块 vs 碎片化海量小文件
            val isDataContentType = uiState.contentType == FillContentType.RAW_FILE ||
                    uiState.contentType == FillContentType.MIXED

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "碎片化小文件模式",
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = when {
                            !isDataContentType -> "当前类型不支持碎片化模式"
                            uiState.isFragmentationMode -> "已开启：生成 ${uiState.fragmentSizeLevel.label} 散列小文件"
                            else -> "关闭：常规连续大文件写入"
                        },
                        fontSize = 13.sp,
                        color = if (uiState.isFragmentationMode) NeonAmber else Color.White.copy(alpha = 0.65f)
                    )
                }
                Switch(
                    checked = uiState.isFragmentationMode,
                    onCheckedChange = onFragmentationToggle,
                    enabled = !isRunning && isDataContentType,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF040D1A),
                        checkedTrackColor = NeonAmber,
                        uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                        uncheckedTrackColor = DarkSurface
                    )
                )
            }

            // 碎片化子配置项
            if (uiState.isFragmentationMode) {
                Spacer(modifier = Modifier.height(14.dp))

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = DarkSurface.copy(alpha = 0.7f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassBorder(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "碎片文件规格等级",
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        FragmentSizeLevel.values().forEach { level ->
                            val isLevelSelected = uiState.fragmentSizeLevel == level
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .bounceClickable(enabled = !isRunning) { onFragmentSizeLevelChange(level) }
                                    .padding(vertical = 5.dp)
                            ) {
                                RadioButton(
                                    selected = isLevelSelected,
                                    onClick = { onFragmentSizeLevelChange(level) },
                                    enabled = !isRunning,
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = NeonAmber,
                                        unselectedColor = Color.White.copy(alpha = 0.4f)
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = level.label,
                                    fontSize = 14.5.sp,
                                    fontWeight = if (isLevelSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isLevelSelected) NeonAmber else Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                        Spacer(modifier = Modifier.height(10.dp))

                        // 穿插打孔选项
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "穿插空洞打孔 (Write & Punch)",
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "每满 500 文件随机剔除 30%，制造物理碎块",
                                    fontSize = 12.5.sp,
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                            }
                            Switch(
                                checked = uiState.isPunchHolesEnabled,
                                onCheckedChange = onPunchHolesToggle,
                                enabled = !isRunning,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFF040D1A),
                                    checkedTrackColor = NeonAmber,
                                    uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                                    uncheckedTrackColor = DarkSurface
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
