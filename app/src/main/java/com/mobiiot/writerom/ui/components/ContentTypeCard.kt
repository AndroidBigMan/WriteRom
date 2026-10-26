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
                        imageVector = Icons.Default.Build,
                        contentDescription = "Content Type",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "填充内容类型与介质",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 单选按钮群 (加大行距与字号)
            FillContentType.values().forEach { type ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .bounceClickable(enabled = !isRunning) { onContentTypeChange(type) }
                        .padding(vertical = 7.dp, horizontal = 4.dp)
                ) {
                    RadioButton(
                        selected = uiState.contentType == type,
                        onClick = { onContentTypeChange(type) },
                        enabled = !isRunning
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = type.displayName,
                        fontSize = 16.sp,
                        fontWeight = if (uiState.contentType == type) FontWeight.Bold else FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(12.dp))

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
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (uiState.isRandomData) "防底层压缩算法 (F2FS)" else "零字节快速写入",
                            fontSize = 13.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = uiState.isRandomData,
                        onCheckedChange = onRandomDataToggle,
                        enabled = !isRunning
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "文件规格与分块大小",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
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
                                text = if (uiState.isFragmentationMode) "碎片化模式生效中" else "${uiState.chunkSizeMb}MB 常规分块",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (uiState.isFragmentationMode) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 碎片化模式选项 Chip
                        val isFragSelected = uiState.isFragmentationMode
                        FilterChip(
                            selected = isFragSelected,
                            onClick = { onFragmentationToggle(true) },
                            label = {
                                Text(
                                    "碎片化",
                                    fontSize = 14.sp,
                                    fontWeight = if (isFragSelected) FontWeight.Bold else FontWeight.Medium
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
                                        fontSize = 14.sp,
                                        fontWeight = if (isChunkSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                enabled = !isRunning,
                                modifier = Modifier
                                    .weight(1f)
                                    .bounceClickable(enabled = !isRunning) { onChunkSizeChange(sizeMb) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(12.dp))
            }

            // 导入系统相册开关
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "导入系统相册",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (uiState.importToMediaGallery) "直接同步至系统相册索引" else "存放在应用专属隔离目录",
                        fontSize = 13.5.sp,
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
