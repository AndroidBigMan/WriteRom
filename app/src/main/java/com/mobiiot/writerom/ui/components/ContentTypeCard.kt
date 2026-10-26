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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobiiot.writerom.model.FillContentType
import com.mobiiot.writerom.model.TaskStatus
import com.mobiiot.writerom.ui.StorageUiState
import com.mobiiot.writerom.ui.theme.bounceClickable
import com.mobiiot.writerom.ui.theme.glassBorder

@Composable
fun ContentTypeCard(
    uiState: StorageUiState,
    onContentTypeChange: (FillContentType) -> Unit,
    onRandomDataToggle: (Boolean) -> Unit,
    onChunkSizeChange: (Int) -> Unit,
    onFragmentationToggle: (Boolean) -> Unit,
    onMediaGalleryToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val isRunning = uiState.taskStatus == TaskStatus.RUNNING || uiState.taskStatus == TaskStatus.PAUSED

    Card(
        modifier = modifier
            .fillMaxWidth()
            .glassBorder(20.dp),
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
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Build,
                        contentDescription = "Content Type",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "填充内容类型与介质",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "高熵随机数据、大文件分块与相册介质",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 单选按钮群
            FillContentType.values().forEach { type ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .bounceClickable(enabled = !isRunning) { onContentTypeChange(type) }
                        .padding(vertical = 4.dp, horizontal = 4.dp)
                ) {
                    RadioButton(
                        selected = uiState.contentType == type,
                        onClick = { onContentTypeChange(type) },
                        enabled = !isRunning
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = type.displayName,
                        fontSize = 14.sp,
                        fontWeight = if (uiState.contentType == type) FontWeight.SemiBold else FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(10.dp))

            // 针对大文件类型的分块设置与随机数据开关
            if (uiState.contentType == FillContentType.RAW_FILE || uiState.contentType == FillContentType.MIXED) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "生成随机高熵数据",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (uiState.isRandomData) "防底层压缩算法（如F2FS），真实消耗闪存" else "零字节填充（写入速度极快）",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = uiState.isRandomData,
                        onCheckedChange = onRandomDataToggle,
                        enabled = !isRunning
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "文件规格与分块大小",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (uiState.isFragmentationMode)
                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                            else
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            modifier = Modifier.bounceClickable(onClick = {})
                        ) {
                            Text(
                                text = if (uiState.isFragmentationMode) "碎片化模式生效中" else "${uiState.chunkSizeMb}MB 常规分块生效中",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (uiState.isFragmentationMode) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // 碎片化模式选项 Chip
                        val isFragSelected = uiState.isFragmentationMode
                        FilterChip(
                            selected = isFragSelected,
                            onClick = { onFragmentationToggle(true) },
                            label = {
                                Text(
                                    "碎片化",
                                    fontSize = 12.sp,
                                    fontWeight = if (isFragSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            enabled = !isRunning,
                            modifier = Modifier
                                .weight(1.1f)
                                .bounceClickable(enabled = !isRunning) { onFragmentationToggle(true) }
                        )

                        // 常规分块大小 Chips (256MB, 500MB, 1024MB)
                        listOf(256, 500, 1024).forEach { sizeMb ->
                            val isChunkSelected = !uiState.isFragmentationMode && uiState.chunkSizeMb == sizeMb
                            FilterChip(
                                selected = isChunkSelected,
                                onClick = { onChunkSizeChange(sizeMb) },
                                label = {
                                    Text(
                                        "${sizeMb}MB",
                                        fontSize = 12.sp,
                                        fontWeight = if (isChunkSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                enabled = !isRunning,
                                modifier = Modifier
                                    .weight(1f)
                                    .bounceClickable(enabled = !isRunning) { onChunkSizeChange(sizeMb) }
                            )
                        }
                    }

                    Text(
                        text = if (uiState.isFragmentationMode)
                            "⚡ 当前生效：碎片化模式 (${uiState.fragmentSizeLevel.label})，生成散列微小文件。可在下方【高级压测】卡片调节等级与打孔。"
                        else
                            "⚡ 当前生效：常规连续大文件写入，单个文件大小上限 ${uiState.chunkSizeMb}MB。",
                        fontSize = 11.sp,
                        color = if (uiState.isFragmentationMode) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(10.dp))
            }

            // 方案A：导入系统相册开关
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "导入系统相册（方案 A 可见性）",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (uiState.importToMediaGallery) "已开启：图片/视频写入相册索引，压测媒体扫描" else "默认关闭：存放在专属隔离目录，不污染个人相册",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = uiState.importToMediaGallery,
                    onCheckedChange = onMediaGalleryToggle,
                    enabled = !isRunning
                )
            }
        }
    }
}
