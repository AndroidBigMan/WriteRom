package com.mobiiot.writerom.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobiiot.writerom.model.TaskStatus
import com.mobiiot.writerom.ui.StorageUiState
import com.mobiiot.writerom.ui.theme.AccentCyan
import com.mobiiot.writerom.ui.theme.ErrorRed
import com.mobiiot.writerom.ui.theme.SuccessGreen
import com.mobiiot.writerom.ui.theme.WarningAmber
import com.mobiiot.writerom.ui.theme.bounceClickable
import com.mobiiot.writerom.ui.theme.glassBorder

/**
 * 底部常驻悬浮控制栏 (Sticky Bottom Action Bar)：
 * 无论滑动至页面何处均吸附于屏幕底端，随手触发开始、暂停、恢复、终止与一键清理。
 */
@Composable
fun StickyBottomActionBar(
    uiState: StorageUiState,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onCleanAll: () -> Unit,
    onStopClean: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showCleanConfirmDialog by remember { mutableStateOf(false) }

    // 清理确认对话框
    if (showCleanConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showCleanConfirmDialog = false },
            title = {
                Text(
                    text = "确认清理填充数据？",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            },
            text = {
                Text(
                    text = "将彻底递归删除应用生成的所有大分块文件、海量散列碎片小文件及多媒体数据，并同步剔除系统相册索引，瞬间释放所有占用的闪存空间。",
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCleanConfirmDialog = false
                        onCleanAll()
                    }
                ) {
                    Text("确认全量清理", color = ErrorRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCleanConfirmDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(16.dp, RoundedCornerShape(22.dp))
            .glassBorder(22.dp),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        tonalElevation = 6.dp
    ) {
        Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            when {
                // ================= 状态 1：正在清理中 =================
                uiState.isCleaning -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.5.dp,
                                color = ErrorRed
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "正在释放数据...",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "已删 ${uiState.cleaningDeletedCount} 文件 · 释放 ${uiState.formattedCleaningReclaimed}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // 停止清理按钮
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = ErrorRed,
                            modifier = Modifier
                                .height(38.dp)
                                .bounceClickable(onClick = onStopClean)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Stop Clean",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("停止清理", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // ================= 状态 2：任务运行中 =================
                uiState.taskStatus == TaskStatus.RUNNING -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 动态数据指标
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(SuccessGreen)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "填充进行中 (${uiState.activeThreads}T)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (uiState.isSlcExhausted) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "⚡SLC耗尽",
                                        fontSize = 10.sp,
                                        color = WarningAmber,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = uiState.formattedSpeed,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentCyan
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "进度 ${uiState.progressPercent.toInt()}% · 剩余 ${uiState.formattedFree}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // 控制按钮组：暂停 + 终止
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // 暂停按钮
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = WarningAmber,
                                modifier = Modifier
                                    .height(42.dp)
                                    .bounceClickable(onClick = onPause)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Pause",
                                        tint = Color.Black,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("暂停", fontSize = 13.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }

                            // 终止任务按钮
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = ErrorRed,
                                modifier = Modifier
                                    .height(42.dp)
                                    .bounceClickable(onClick = onStop)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Stop",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("终止", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // ================= 状态 3：任务已暂停 =================
                uiState.taskStatus == TaskStatus.PAUSED -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(WarningAmber)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "任务已暂停",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WarningAmber
                                )
                            }
                            Text(
                                text = "已写入 ${uiState.formattedWritten} / ${uiState.formattedTarget}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // 恢复按钮
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = SuccessGreen,
                                modifier = Modifier
                                    .height(42.dp)
                                    .bounceClickable(onClick = onResume)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Resume",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("恢复", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }

                            // 终止按钮
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = ErrorRed,
                                modifier = Modifier
                                    .height(42.dp)
                                    .bounceClickable(onClick = onStop)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Stop",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("终止", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // ================= 状态 4：空闲 / 完成 / 停止状态 (默认主操作) =================
                else -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 快捷一键清理入口图标按钮
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ErrorRed.copy(alpha = 0.25f)),
                            modifier = Modifier
                                .height(50.dp)
                                .width(54.dp)
                                .bounceClickable(
                                    enabled = !uiState.isCleaning,
                                    onClick = { showCleanConfirmDialog = true }
                                )
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Clean All",
                                    tint = ErrorRed,
                                    modifier = Modifier.size(20.dp)
                                )
                                if (uiState.existingFillerFilesCount > 0) {
                                    Text(
                                        text = "${uiState.existingFillerFilesCount}",
                                        fontSize = 10.sp,
                                        color = ErrorRed,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // 主操作开始填充按钮（带弹簧阻尼微动效）
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = SuccessGreen,
                            shadowElevation = 4.dp,
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .bounceClickable(onClick = onStart)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Start",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(horizontalAlignment = Alignment.Start) {
                                    Text(
                                        text = if (uiState.taskStatus == TaskStatus.IDLE) "开始填充 ROM" else "重新开始填充",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "预计写入 ${uiState.formattedEstimatedFill} · ${uiState.effectiveWriteSpec}",
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.85f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
