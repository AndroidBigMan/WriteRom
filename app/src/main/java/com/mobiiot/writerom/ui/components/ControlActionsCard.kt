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
import com.mobiiot.writerom.ui.theme.bounceClickable
import com.mobiiot.writerom.ui.theme.glassBorder

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
            Text(
                text = "任务控制与空间释放",
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(18.dp))

            // 主任务控制按钮 (高度提升至 58dp，字号提升至 17~18sp)
            when (uiState.taskStatus) {
                TaskStatus.IDLE, TaskStatus.COMPLETED, TaskStatus.STOPPED, TaskStatus.ERROR -> {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = SuccessGreen,
                        shadowElevation = 3.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp)
                            .bounceClickable(onClick = onStart)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Start", tint = Color.White, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (uiState.taskStatus == TaskStatus.IDLE) "开始填充 ROM" else "重新开始填充",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                TaskStatus.RUNNING -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = WarningAmber,
                            shadowElevation = 2.dp,
                            modifier = Modifier
                                .weight(1f)
                                .height(58.dp)
                                .bounceClickable(onClick = onPause)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = Icons.Default.Refresh, contentDescription = "Pause", tint = Color.Black, modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("暂停", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = ErrorRed,
                            shadowElevation = 2.dp,
                            modifier = Modifier
                                .weight(1f)
                                .height(58.dp)
                                .bounceClickable(onClick = onStop)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Stop", tint = Color.White, modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("终止任务", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }

                TaskStatus.PAUSED -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = SuccessGreen,
                            shadowElevation = 2.dp,
                            modifier = Modifier
                                .weight(1f)
                                .height(58.dp)
                                .bounceClickable(onClick = onResume)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Resume", tint = Color.White, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("继续恢复", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = ErrorRed,
                            shadowElevation = 2.dp,
                            modifier = Modifier
                                .weight(1f)
                                .height(58.dp)
                                .bounceClickable(onClick = onStop)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Stop", tint = Color.White, modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("终止任务", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 清理中状态面板 或 一键清理按钮
            if (uiState.isCleaning) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = ErrorRed.copy(alpha = 0.12f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassBorder(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 3.dp,
                                    color = ErrorRed
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "正在深度释放闪存空间...",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Text(
                                text = "已释放: ${uiState.formattedCleaningReclaimed}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = ErrorRed
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "已删除文件: ${uiState.cleaningDeletedCount} 个",
                            fontSize = 13.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        if (uiState.existingFillerFilesCount > 0) {
                            val cleanFraction = (uiState.cleaningDeletedCount.toFloat() / uiState.existingFillerFilesCount).coerceIn(0f, 1f)
                            LinearProgressIndicator(
                                progress = { cleanFraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(16.dp),
                                color = ErrorRed,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        } else {
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(16.dp),
                                color = ErrorRed,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = ErrorRed,
                            shadowElevation = 2.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .bounceClickable(onClick = onStopClean)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Stop Clean", tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("停止清理 (安全保留已释放部分)", fontSize = 16.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .glassBorder(16.dp)
                        .bounceClickable(
                            enabled = uiState.taskStatus != TaskStatus.RUNNING,
                            onClick = { showCleanConfirmDialog = true }
                        )
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Clean", tint = ErrorRed, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "一键清理已填充的 ROM 空间",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (uiState.taskStatus == TaskStatus.RUNNING) MaterialTheme.colorScheme.onSurfaceVariant else ErrorRed
                        )
                    }
                }
            }
        }
    }

    if (showCleanConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showCleanConfirmDialog = false },
            title = {
                Text(
                    text = "确认清理释放？",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = "将彻底递归删除应用生成的所有大文件与微小碎片，并同步剔除相册索引，瞬间释放所有占用的闪存空间。",
                    fontSize = 15.sp,
                    lineHeight = 22.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCleanConfirmDialog = false
                        onCleanAll()
                    }
                ) {
                    Text("确认全量清理", color = ErrorRed, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCleanConfirmDialog = false }) {
                    Text("取消", fontSize = 15.sp)
                }
            }
        )
    }
}
