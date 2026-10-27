package com.mobiiot.writerom.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobiiot.writerom.model.TaskStatus
import com.mobiiot.writerom.ui.StorageUiState
import com.mobiiot.writerom.ui.theme.DarkSurface
import com.mobiiot.writerom.ui.theme.ElectricBlue
import com.mobiiot.writerom.ui.theme.NeonAmber
import com.mobiiot.writerom.ui.theme.NeonCyan
import com.mobiiot.writerom.ui.theme.NeonEmerald
import com.mobiiot.writerom.ui.theme.NeonRose
import com.mobiiot.writerom.ui.theme.bounceClickable
import com.mobiiot.writerom.ui.theme.glassBorder

/**
 * 屏幕底部常驻半透明悬浮控制岛 (Sticky Bottom Action Bar)
 * Direction C: Modern Neon Glassmorphic Bento
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

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(24.dp, RoundedCornerShape(26.dp), spotColor = NeonCyan.copy(alpha = 0.22f))
            .glassBorder(26.dp),
        shape = RoundedCornerShape(26.dp),
        color = DarkSurface.copy(alpha = 0.94f),
        tonalElevation = 8.dp
    ) {
        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
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
                                modifier = Modifier.size(26.dp),
                                strokeWidth = 3.dp,
                                color = NeonRose
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "正在深度释放数据...",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "已删 ${uiState.cleaningDeletedCount} 文件 · 释放 ${uiState.formattedCleaningReclaimed}",
                                    fontSize = 13.sp,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            }
                        }

                        // 停止清理按钮
                        Box(
                            modifier = Modifier
                                .height(46.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Brush.horizontalGradient(listOf(NeonRose, Color(0xFFB91C1C))))
                                .bounceClickable(onClick = onStopClean),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Stop Clean",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("停止清理", fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Bold)
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
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(NeonEmerald)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "填充进行中 (${uiState.activeThreads}T)",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                if (uiState.isSlcExhausted) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(NeonAmber.copy(alpha = 0.18f))
                                            .border(1.dp, NeonAmber.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "⚡SLC耗尽",
                                            fontSize = 11.sp,
                                            color = NeonAmber,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = uiState.formattedSpeed,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = NeonCyan
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "进度 ${uiState.progressPercent.toInt()}% · 剩余 ${uiState.formattedFree}",
                                    fontSize = 13.sp,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            }
                        }

                        // 控制按钮组：暂停 + 终止
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            // 暂停按钮 (Neon Amber)
                            Box(
                                modifier = Modifier
                                    .height(46.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Brush.horizontalGradient(listOf(NeonAmber, Color(0xFFD97706))))
                                    .bounceClickable(onClick = onPause),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Pause",
                                        tint = Color(0xFF040D1A),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("暂停", fontSize = 15.sp, color = Color(0xFF040D1A), fontWeight = FontWeight.Bold)
                                }
                            }

                            // 终止任务按钮 (Neon Rose)
                            Box(
                                modifier = Modifier
                                    .height(46.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Brush.horizontalGradient(listOf(NeonRose, Color(0xFFDC2626))))
                                    .bounceClickable(onClick = onStop),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Stop",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("终止", fontSize = 15.sp, color = Color.White, fontWeight = FontWeight.Bold)
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
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(NeonAmber)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "任务已暂停",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonAmber
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "已写入 ${uiState.formattedWritten} / ${uiState.formattedTarget}",
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            // 恢复按钮 (Neon Emerald -> Cyan)
                            Box(
                                modifier = Modifier
                                    .height(46.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Brush.horizontalGradient(listOf(NeonEmerald, NeonCyan)))
                                    .bounceClickable(onClick = onResume),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Resume",
                                        tint = Color(0xFF040D1A),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("恢复", fontSize = 15.sp, color = Color(0xFF040D1A), fontWeight = FontWeight.Bold)
                                }
                            }

                            // 终止按钮 (Neon Rose)
                            Box(
                                modifier = Modifier
                                    .height(46.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Brush.horizontalGradient(listOf(NeonRose, Color(0xFFDC2626))))
                                    .bounceClickable(onClick = onStop),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Stop",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("终止", fontSize = 15.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // ================= 状态 4：空闲 / 完成 / 停止状态 (默认主操作) =================
                else -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 快捷一键清理入口图标按钮
                        Box(
                            modifier = Modifier
                                .height(56.dp)
                                .width(60.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .border(1.dp, NeonRose.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                                .bounceClickable(
                                    enabled = !uiState.isCleaning,
                                    onClick = { showCleanConfirmDialog = true }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Clean All",
                                    tint = NeonRose,
                                    modifier = Modifier.size(22.dp)
                                )
                                if (uiState.existingFillerFilesCount > 0) {
                                    Text(
                                        text = "${uiState.existingFillerFilesCount}",
                                        fontSize = 11.sp,
                                        color = NeonRose,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // 主操作开始填充按钮 (Neon Cyan -> Electric Blue 流光)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Brush.horizontalGradient(listOf(NeonCyan, ElectricBlue)))
                                .bounceClickable(onClick = onStart),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 18.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Start",
                                    tint = Color(0xFF040D1A),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(horizontalAlignment = Alignment.Start) {
                                    Text(
                                        text = if (uiState.taskStatus == TaskStatus.IDLE) "开始填充 ROM" else "重新开始填充",
                                        fontSize = 16.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF040D1A)
                                    )
                                    Text(
                                        text = "预计写入 ${uiState.formattedEstimatedFill} · ${uiState.effectiveWriteSpec}",
                                        fontSize = 12.5.sp,
                                        color = Color(0xFF040D1A).copy(alpha = 0.82f)
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
