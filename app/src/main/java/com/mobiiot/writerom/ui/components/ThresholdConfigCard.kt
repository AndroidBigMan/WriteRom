package com.mobiiot.writerom.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobiiot.writerom.model.FillMode
import com.mobiiot.writerom.model.TaskStatus
import com.mobiiot.writerom.ui.StorageUiState
import com.mobiiot.writerom.utils.StorageHelper

@Composable
fun ThresholdConfigCard(
    uiState: StorageUiState,
    onModeChange: (FillMode) -> Unit,
    onRemainingBytesChange: (Long) -> Unit,
    onPercentageChange: (Int) -> Unit,
    onSpecificSizeChange: (Long) -> Unit,
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
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Threshold",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "填充阈值与策略设置",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 模式选择分段组件
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(4.dp)
            ) {
                FillMode.values().forEach { mode ->
                    val isSelected = uiState.fillMode == mode
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                            )
                            .clickable(enabled = !isRunning) { onModeChange(mode) }
                            .padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = mode.displayName,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 针对不同模式的具体滑块与设置
            when (uiState.fillMode) {
                FillMode.TARGET_REMAINING_SPACE -> {
                    val maxRemainingGb = (uiState.freeBytes / (1024.0 * 1024 * 1024)).toFloat()
                    val currentRemainingGb = (uiState.targetRemainingBytes / (1024.0 * 1024 * 1024)).toFloat()

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "目标保留剩余空间",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (uiState.targetRemainingBytes <= 0) "0 MB (极限压测，完全填满)" else StorageHelper.formatSize(uiState.targetRemainingBytes),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Slider(
                            value = currentRemainingGb.coerceIn(0f, maxRemainingGb.coerceAtLeast(0.1f)),
                            onValueChange = { newVal ->
                                val bytes = (newVal * 1024 * 1024 * 1024).toLong()
                                onRemainingBytesChange(bytes)
                            },
                            valueRange = 0f..maxRemainingGb.coerceAtLeast(0.1f),
                            enabled = !isRunning,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(
                            text = "注：默认安全保留 0 MB，将一直写入直到闪存极限耗尽。",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                FillMode.TARGET_PERCENTAGE -> {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "目标总占用百分比",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${uiState.targetPercentage}%",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        val minPercent = uiState.percentUsed.toInt().coerceIn(1, 100)
                        Slider(
                            value = uiState.targetPercentage.toFloat().coerceIn(minPercent.toFloat(), 100f),
                            onValueChange = { onPercentageChange(it.toInt()) },
                            valueRange = minPercent.toFloat()..100f,
                            enabled = !isRunning,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(
                            text = "当前已使用 ${String.format("%.1f", uiState.percentUsed)}%，可设置向上填充至最高 100%。",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                FillMode.TARGET_SPECIFIC_SIZE -> {
                    val maxFillGb = (uiState.freeBytes / (1024.0 * 1024 * 1024)).toFloat()
                    val currentFillGb = (uiState.targetSpecificSizeBytes / (1024.0 * 1024 * 1024)).toFloat()

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "指定填充数据量",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = StorageHelper.formatSize(uiState.targetSpecificSizeBytes),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Slider(
                            value = currentFillGb.coerceIn(0.1f, maxFillGb.coerceAtLeast(0.1f)),
                            onValueChange = { newVal ->
                                val bytes = (newVal * 1024 * 1024 * 1024).toLong()
                                onSpecificSizeChange(bytes)
                            },
                            valueRange = 0.1f..maxFillGb.coerceAtLeast(0.1f),
                            enabled = !isRunning,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(
                            text = "受限于手机当前实际可用容量，最高不可超过 ${uiState.formattedFree}。",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 预估填充体积反馈条
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "本次预计将填充体积",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = uiState.formattedEstimatedFill,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
