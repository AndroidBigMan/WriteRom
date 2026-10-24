package com.mobiiot.writerom.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Info
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
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Build,
                        contentDescription = "Advanced",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "高级压测与并发设置",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "v1.2 增强",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 1. 并发线程数设置 (1 ~ 8)
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "多线程并发写入",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${uiState.concurrencyThreads} 线程并发",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(1, 2, 4, 8).forEach { t ->
                        val isSelected = uiState.concurrencyThreads == t
                        FilterChip(
                            selected = isSelected,
                            onClick = { onConcurrencyChange(t) },
                            label = { Text("${t}T", fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            enabled = !isRunning,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Text(
                    text = when (uiState.concurrencyThreads) {
                        1 -> "单线程：温和稳定占位，设备低发热"
                        2 -> "双线程：平衡吞吐，适合常规压力测试"
                        4 -> "4 线程：高速并发，充分利用多核与多通道闪存"
                        else -> "8 线程：极限满载压测，测试高 I/O 竞争与降频表现"
                    },
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
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
                        text = "碎片化海量小文件模式",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = when {
                            !isDataContentType -> "不可用：当前已选【${uiState.contentType.displayName}】，碎片化仅支持普通数据文件"
                            uiState.isFragmentationMode -> "已开启：接管上方分块大小，以 ${uiState.fragmentSizeLevel.label} 散列微小文件进行填充"
                            else -> "关闭：遵循上方分块设置（当前生效：${uiState.chunkSizeMb}MB 连续写入）"
                        },
                        fontSize = 11.sp,
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
                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "碎片文件规格等级",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        FragmentSizeLevel.values().forEach { level ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable(enabled = !isRunning) { onFragmentSizeLevelChange(level) }
                                    .padding(vertical = 3.dp)
                            ) {
                                RadioButton(
                                    selected = uiState.fragmentSizeLevel == level,
                                    onClick = { onFragmentSizeLevelChange(level) },
                                    enabled = !isRunning
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = level.label,
                                    fontSize = 12.sp,
                                    fontWeight = if (uiState.fragmentSizeLevel == level) FontWeight.Bold else FontWeight.Normal,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // 穿插打孔选项
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "穿插空洞打孔 (Write & Punch)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "每写满500个小文件随机剔除30%，在闪存块中制造真实的物理不连续碎片",
                                    fontSize = 10.sp,
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
