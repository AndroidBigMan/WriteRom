package com.mobiiot.writerom.ui.components

import androidx.compose.foundation.background
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
import com.mobiiot.writerom.model.FragmentSizeLevel
import com.mobiiot.writerom.model.TaskStatus
import com.mobiiot.writerom.ui.StorageUiState
import com.mobiiot.writerom.ui.theme.bounceClickable
import com.mobiiot.writerom.ui.theme.glassBorder

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
            .glassBorder(22.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp)
        ) {
            // 标题栏与并发碎片状态胶囊（上下平行排列）
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Build,
                            contentDescription = "Advanced",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "高级压测与并发设置",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    modifier = Modifier.bounceClickable(onClick = {})
                ) {
                    Text(
                        text = "并发与碎片",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
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
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${uiState.concurrencyThreads} 线程并发",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
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
                            label = { Text("${t}T", fontSize = 14.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium) },
                            enabled = !isRunning,
                            modifier = Modifier
                                .weight(1f)
                                .bounceClickable(enabled = !isRunning) { onConcurrencyChange(t) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(14.dp))

            // 2. 写入模式切换：常规顺序大块 vs 碎片化海量小文件
            val isDataContentType = uiState.contentType == com.mobiiot.writerom.model.FillContentType.RAW_FILE ||
                    uiState.contentType == com.mobiiot.writerom.model.FillContentType.MIXED

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "碎片化小文件模式",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = when {
                            !isDataContentType -> "当前类型不支持碎片化模式"
                            uiState.isFragmentationMode -> "已开启：生成 ${uiState.fragmentSizeLevel.label} 散列小文件"
                            else -> "关闭：常规连续大文件写入"
                        },
                        fontSize = 13.5.sp,
                        color = if (uiState.isFragmentationMode) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = uiState.isFragmentationMode,
                    onCheckedChange = onFragmentationToggle,
                    enabled = !isRunning && isDataContentType
                )
            }

            // 碎片化子配置项
            if (uiState.isFragmentationMode) {
                Spacer(modifier = Modifier.height(14.dp))

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassBorder(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "碎片文件规格等级",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        FragmentSizeLevel.values().forEach { level ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .bounceClickable(enabled = !isRunning) { onFragmentSizeLevelChange(level) }
                                    .padding(vertical = 5.dp)
                            ) {
                                RadioButton(
                                    selected = uiState.fragmentSizeLevel == level,
                                    onClick = { onFragmentSizeLevelChange(level) },
                                    enabled = !isRunning
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = level.label,
                                    fontSize = 15.sp,
                                    fontWeight = if (uiState.fragmentSizeLevel == level) FontWeight.Bold else FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

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
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "每满 500 文件随机剔除 30%，制造物理碎块",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = uiState.isPunchHolesEnabled,
                                onCheckedChange = onPunchHolesToggle,
                                enabled = !isRunning
                            )
                        }
                    }
                }
            }
        }
    }
}
