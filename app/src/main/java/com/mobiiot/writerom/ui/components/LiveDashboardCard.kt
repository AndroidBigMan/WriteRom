package com.mobiiot.writerom.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobiiot.writerom.model.TaskStatus
import com.mobiiot.writerom.ui.StorageUiState
import com.mobiiot.writerom.ui.theme.*

/**
 * 方案 C: 现代流光工学派 (Neon Live Dashboard Card)
 * 实时多线程并发、瞬时写入吞吐量与进度看板
 */
@Composable
fun LiveDashboardCard(
    uiState: StorageUiState,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = uiState.taskStatus != TaskStatus.IDLE,
        modifier = modifier.fillMaxWidth()
    ) {
        val statusColor = when (uiState.taskStatus) {
            TaskStatus.RUNNING -> NeonEmerald
            TaskStatus.PAUSED -> NeonAmber
            TaskStatus.COMPLETED -> NeonCyan
            TaskStatus.ERROR -> NeonRose
            else -> NeonCyan
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .glassBorder(24.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
            ) {
                // 顶部状态栏（上下平行排列）
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
                                .size(46.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(statusColor.copy(alpha = 0.22f), ElectricBlue.copy(alpha = 0.10f))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Live Status",
                                tint = statusColor,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "实时写入监控",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                letterSpacing = (-0.02).sp
                            )
                            Text(
                                text = "多核吞吐与任务执行状态",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = NeonCyan.copy(alpha = 0.12f),
                            modifier = Modifier
                                .glassBorder(12.dp, 0.6.dp)
                                .bounceClickable(onClick = {})
                        ) {
                            Text(
                                text = "⚡ ${uiState.activeThreads} 线程并发",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = statusColor.copy(alpha = 0.15f),
                            modifier = Modifier
                                .glassBorder(12.dp, 0.6.dp)
                                .bounceClickable(onClick = {})
                        ) {
                            Text(
                                text = uiState.taskStatus.label,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = statusColor,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 瞬时写入速度大字
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = "瞬时写入速率",
                            fontSize = 13.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = uiState.formattedSpeed,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = statusColor,
                            letterSpacing = (-0.03).sp
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "任务总进度",
                            fontSize = 13.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = String.format("%.1f%%", uiState.progressPercent),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 进度条 (带霓虹渐变与顶部高光)
                val animatedProgress by animateFloatAsState(
                    targetValue = (uiState.progressPercent / 100f).coerceIn(0f, 1f),
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    label = "taskProgress"
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(22.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                        .glassBorder(cornerRadius = 11.dp, strokeWidth = 0.5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedProgress)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(11.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(NeonCyan, ElectricBlue)
                                )
                            )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color.White.copy(alpha = 0.35f),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 统计网格 4 项 Bento 容器
                val formattedCount = java.text.NumberFormat.getIntegerInstance().format(uiState.filesCreatedCount)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    BentoMetricTile(
                        label = "已写入数据",
                        value = "${uiState.formattedWritten} / ${uiState.formattedTarget}",
                        accent = NeonCyan,
                        modifier = Modifier.weight(1f)
                    )
                    BentoMetricTile(
                        label = "已生成文件/记录数",
                        value = "$formattedCount 个",
                        accent = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    BentoMetricTile(
                        label = "任务已耗时",
                        value = uiState.formattedElapsed,
                        accent = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    BentoMetricTile(
                        label = "预计剩余时间 (ETA)",
                        value = uiState.formattedEta,
                        accent = NeonAmber,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (uiState.currentFileName.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        modifier = Modifier.fillMaxWidth().glassBorder(10.dp, 0.5.dp)
                    ) {
                        Text(
                            text = "当前写入: ${uiState.currentFileName}",
                            fontSize = 12.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        )
                    }
                }

                if (uiState.statusMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = uiState.statusMessage,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }
            }
        }
    }
}

@Composable
private fun BentoMetricTile(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.40f),
        modifier = modifier
            .glassBorder(16.dp, 0.8.dp)
            .bounceClickable(onClick = {})
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp)) {
            Text(
                text = label,
                fontSize = 12.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = accent
            )
        }
    }
}
