package com.mobiiot.writerom.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobiiot.writerom.model.TaskStatus
import com.mobiiot.writerom.ui.StorageUiState
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
 * 任务控制与空间释放卡片
 * Direction C: Modern Neon Glassmorphic Bento
 */
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
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(NeonCyan.copy(alpha = 0.12f))
                        .border(1.dp, NeonCyan.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Actions",
                        tint = NeonCyan,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "任务控制与空间释放",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 主任务控制按钮
            when (uiState.taskStatus) {
                TaskStatus.IDLE, TaskStatus.COMPLETED, TaskStatus.STOPPED, TaskStatus.ERROR -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Brush.horizontalGradient(listOf(NeonCyan, ElectricBlue)))
                            .bounceClickable(onClick = onStart),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Start",
                                tint = Color(0xFF040D1A),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (uiState.taskStatus == TaskStatus.IDLE) "开始填充 ROM" else "重新开始填充",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF040D1A)
                            )
                        }
                    }
                }

                TaskStatus.RUNNING -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Brush.horizontalGradient(listOf(NeonAmber, Color(0xFFD97706))))
                                .bounceClickable(onClick = onPause),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Pause",
                                    tint = Color(0xFF040D1A),
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("暂停任务", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF040D1A))
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Brush.horizontalGradient(listOf(NeonRose, Color(0xFFDC2626))))
                                .bounceClickable(onClick = onStop),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Stop",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("终止任务", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }

                TaskStatus.PAUSED -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Brush.horizontalGradient(listOf(NeonEmerald, NeonCyan)))
                                .bounceClickable(onClick = onResume),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Resume",
                                    tint = Color(0xFF040D1A),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("继续恢复", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF040D1A))
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Brush.horizontalGradient(listOf(NeonRose, Color(0xFFDC2626))))
                                .bounceClickable(onClick = onStop),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Stop",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("终止任务", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
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
                    color = NeonRose.copy(alpha = 0.10f),
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
                                    color = NeonRose
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "正在深度释放闪存空间...",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color.White
                                )
                            }

                            Text(
                                text = "已释放: ${uiState.formattedCleaningReclaimed}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = NeonRose
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "已删除文件: ${uiState.cleaningDeletedCount} 个",
                            fontSize = 13.5.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        if (uiState.existingFillerFilesCount > 0) {
                            val cleanFraction = (uiState.cleaningDeletedCount.toFloat() / uiState.existingFillerFilesCount).coerceIn(0f, 1f)
                            LinearProgressIndicator(
                                progress = { cleanFraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(12.dp)
                                    .clip(RoundedCornerShape(6.dp)),
                                color = NeonRose,
                                trackColor = DarkSurface
                            )
                        } else {
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(12.dp)
                                    .clip(RoundedCornerShape(6.dp)),
                                color = NeonRose,
                                trackColor = DarkSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Brush.horizontalGradient(listOf(NeonRose, Color(0xFFB91C1C))))
                                .bounceClickable(onClick = onStopClean),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Stop Clean",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("停止清理 (保留已释放部分)", fontSize = 15.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.04f))
                        .border(1.dp, NeonRose.copy(alpha = 0.32f), RoundedCornerShape(16.dp))
                        .bounceClickable(
                            enabled = uiState.taskStatus != TaskStatus.RUNNING,
                            onClick = { showCleanConfirmDialog = true }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Clean",
                            tint = NeonRose,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "一键清理已填充的 ROM 空间",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (uiState.taskStatus == TaskStatus.RUNNING) Color.White.copy(alpha = 0.4f) else NeonRose
                        )
                    }
                }
            }
        }
    }

    if (showCleanConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showCleanConfirmDialog = false },
            containerColor = DarkSurface,
            title = {
                Text(
                    text = "确认清理释放？",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.White
                )
            },
            text = {
                Text(
                    text = "将彻底递归删除应用生成的所有大文件与微小碎片，并同步剔除相册索引，瞬间释放所有占用的闪存空间。",
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    color = Color.White.copy(alpha = 0.75f)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCleanConfirmDialog = false
                        onCleanAll()
                    }
                ) {
                    Text("确认全量清理", color = NeonRose, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCleanConfirmDialog = false }) {
                    Text("取消", fontSize = 15.sp, color = Color.White.copy(alpha = 0.6f))
                }
            }
        )
    }
}
