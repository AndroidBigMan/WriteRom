package com.mobiiot.writerom.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobiiot.writerom.model.TaskStatus
import com.mobiiot.writerom.ui.StorageUiState
import com.mobiiot.writerom.ui.theme.ErrorRed
import com.mobiiot.writerom.ui.theme.SuccessGreen
import com.mobiiot.writerom.ui.theme.WarningAmber

@Composable
fun ControlActionsCard(
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
            Text(
                text = "任务控制与空间释放",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 主任务控制按钮
            when (uiState.taskStatus) {
                TaskStatus.IDLE, TaskStatus.COMPLETED, TaskStatus.STOPPED, TaskStatus.ERROR -> {
                    Button(
                        onClick = onStart,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Start")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (uiState.taskStatus == TaskStatus.IDLE) "开始填充 ROM" else "重新开始填充",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                TaskStatus.RUNNING -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = onPause,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = WarningAmber),
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Pause")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("暂停", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = onStop,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Stop")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("终止任务", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                TaskStatus.PAUSED -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = onResume,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Resume")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("恢复填充", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = onStop,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Stop")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("终止任务", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 清理与空间释放控制
            if (uiState.isCleaning) {
                // 清理中状态面板：包含明确进度信息与停止清除按钮
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = ErrorRed.copy(alpha = 0.08f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ErrorRed.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.5.dp,
                                    color = ErrorRed
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "正在清理释放空间...",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            // 停止清理按钮
                            Button(
                                onClick = onStopClean,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Stop Clean",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("停止清理", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // 进度与释放统计
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val countText = if (uiState.existingFillerFilesCount > 0) {
                                "已清理 ${uiState.cleaningDeletedCount} / ${uiState.existingFillerFilesCount} 个文件"
                            } else {
                                "已清理 ${uiState.cleaningDeletedCount} 个文件"
                            }
                            Text(
                                text = countText,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "已释放 ${uiState.formattedCleaningReclaimed}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // 动态进度条
                        if (uiState.existingFillerFilesCount > 0) {
                            val progress = (uiState.cleaningDeletedCount.toFloat() / uiState.existingFillerFilesCount.toFloat()).coerceIn(0f, 1f)
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp),
                                color = ErrorRed,
                                trackColor = ErrorRed.copy(alpha = 0.2f),
                            )
                        } else {
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp),
                                color = ErrorRed,
                                trackColor = ErrorRed.copy(alpha = 0.2f),
                            )
                        }
                    }
                }
            } else {
                // 一键清理按钮（仅在未运行或已停止时允许）
                val canClean = uiState.taskStatus != TaskStatus.RUNNING
                OutlinedButton(
                    onClick = { showCleanConfirmDialog = true },
                    enabled = canClean,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = ErrorRed
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Clean")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (uiState.existingFillerFilesCount > 0)
                            "一键清理填充数据 (${uiState.existingFillerFilesCount} 个文件)"
                        else
                            "一键清理填充数据（恢复空间）",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }

    if (showCleanConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showCleanConfirmDialog = false },
            title = {
                Text(text = "确认清理填充数据？", fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    text = "此操作将永久删除本应用在隔离目录中生成的所有填充文件（包括文件、图片、视频），并立即恢复手机可用 ROM 空间。\n\n不会删除您的任何个人文件。"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCleanConfirmDialog = false
                        onCleanAll()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) {
                    Text("确认清理")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCleanConfirmDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}
