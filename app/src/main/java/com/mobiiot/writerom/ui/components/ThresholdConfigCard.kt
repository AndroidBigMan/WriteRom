package com.mobiiot.writerom.ui.components

import androidx.compose.foundation.background
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
import com.mobiiot.writerom.ui.theme.bounceClickable
import com.mobiiot.writerom.ui.theme.glassBorder
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
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Threshold",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "填充阈值与策略设置",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 模式选择分段组件 (加大触控区域与字号)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                    .glassBorder(14.dp)
                    .padding(5.dp)
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
                            .bounceClickable(enabled = !isRunning) { onModeChange(mode) }
                            .padding(vertical = 12.dp)
                    ) {
                        Text(
                            text = mode.displayName,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 针对不同模式的具体滑块与设置 (精简冗余文字，放大主标签与数值)
            when (uiState.fillMode) {
                FillMode.TARGET_REMAINING_SPACE -> {
                    val maxRemainingGb = (uiState.freeBytes / (1024.0 * 1024 * 1024)).toFloat()
                    val currentRemainingGb = (uiState.targetRemainingBytes / (1024.0 * 1024 * 1024)).toFloat()

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "目标保留剩余空间",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (uiState.targetRemainingBytes <= 0) "0 MB (极限填满)" else StorageHelper.formatSize(uiState.targetRemainingBytes),
                                fontSize = 16.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

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
                    }
                }

                FillMode.TARGET_PERCENTAGE -> {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "目标总占用百分比",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${uiState.targetPercentage}%",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        val minPercent = uiState.percentUsed.toInt().coerceIn(1, 100)
                        Slider(
                            value = uiState.targetPercentage.toFloat().coerceIn(minPercent.toFloat(), 100f),
                            onValueChange = { onPercentageChange(it.toInt()) },
                            valueRange = minPercent.toFloat()..100f,
                            enabled = !isRunning,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                FillMode.TARGET_SPECIFIC_SIZE -> {
                    val maxFillGb = (uiState.freeBytes / (1024.0 * 1024 * 1024)).toFloat()
                    val currentFillGb = (uiState.targetSpecificSizeBytes / (1024.0 * 1024 * 1024)).toFloat()

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "指定填充数据量",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = StorageHelper.formatSize(uiState.targetSpecificSizeBytes),
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

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
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 预估填充体积反馈条 (放大字号)
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                modifier = Modifier
                    .fillMaxWidth()
                    .glassBorder(14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 13.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "本次预计将填充体积",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = uiState.formattedEstimatedFill,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
