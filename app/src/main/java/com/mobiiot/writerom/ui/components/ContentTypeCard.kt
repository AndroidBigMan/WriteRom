package com.mobiiot.writerom.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobiiot.writerom.model.FillContentType
import com.mobiiot.writerom.model.TaskStatus
import com.mobiiot.writerom.ui.StorageUiState
import com.mobiiot.writerom.ui.theme.DarkGlassSurface
import com.mobiiot.writerom.ui.theme.DarkSurface
import com.mobiiot.writerom.ui.theme.NeonAmber
import com.mobiiot.writerom.ui.theme.NeonCyan
import com.mobiiot.writerom.ui.theme.bounceClickable
import com.mobiiot.writerom.ui.theme.glassBorder

/**
 * 填充内容类型与介质卡片
 * Direction C: Modern Neon Glassmorphic Bento
 */
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
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
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
                        imageVector = Icons.Default.Build,
                        contentDescription = "Content Type",
                        tint = NeonCyan,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "填充内容类型与介质",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 单选按钮群
            FillContentType.values().forEach { type ->
                val isSelected = uiState.contentType == type
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) NeonCyan.copy(alpha = 0.08f) else Color.Transparent
                        )
                        .then(
                            if (isSelected) Modifier.border(1.dp, NeonCyan.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                            else Modifier
                        )
                        .bounceClickable(enabled = !isRunning) { onContentTypeChange(type) }
                        .padding(vertical = 8.dp, horizontal = 10.dp)
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = { onContentTypeChange(type) },
                        enabled = !isRunning,
                        colors = RadioButtonDefaults.colors(
                            selectedColor = NeonCyan,
                            unselectedColor = Color.White.copy(alpha = 0.4f)
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = type.displayName,
                        fontSize = 15.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) NeonCyan else Color.White.copy(alpha = 0.85f)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
            Spacer(modifier = Modifier.height(14.dp))

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
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (uiState.isRandomData) "防底层压缩算法 (F2FS)" else "零字节快速写入",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.65f)
                        )
                    }
                    Switch(
                        checked = uiState.isRandomData,
                        onCheckedChange = onRandomDataToggle,
                        enabled = !isRunning,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF040D1A),
                            checkedTrackColor = NeonCyan,
                            uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                            uncheckedTrackColor = DarkSurface
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "文件规格与分块大小",
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (uiState.isFragmentationMode)
                                NeonAmber.copy(alpha = 0.15f)
                            else
                                NeonCyan.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = if (uiState.isFragmentationMode) "碎片化模式生效中" else "${uiState.chunkSizeMb}MB 常规分块",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (uiState.isFragmentationMode) NeonAmber else NeonCyan,
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
                                    fontSize = 13.5.sp,
                                    fontWeight = if (isFragSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            enabled = !isRunning,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeonAmber.copy(alpha = 0.18f),
                                selectedLabelColor = NeonAmber,
                                containerColor = DarkSurface,
                                labelColor = Color.White.copy(alpha = 0.7f)
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = !isRunning,
                                selected = isFragSelected,
                                borderColor = if (isFragSelected) NeonAmber.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.1f)
                            ),
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
                                        fontSize = 13.5.sp,
                                        fontWeight = if (isChunkSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                enabled = !isRunning,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NeonCyan.copy(alpha = 0.18f),
                                    selectedLabelColor = NeonCyan,
                                    containerColor = DarkSurface,
                                    labelColor = Color.White.copy(alpha = 0.7f)
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = !isRunning,
                                    selected = isChunkSelected,
                                    borderColor = if (isChunkSelected) NeonCyan.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.1f)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .bounceClickable(enabled = !isRunning) { onChunkSizeChange(sizeMb) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                Spacer(modifier = Modifier.height(14.dp))
            }

            // 导入系统相册开关 (仅在包含多媒体时展示)
            if (uiState.contentType == FillContentType.IMAGE || uiState.contentType == FillContentType.VIDEO || uiState.contentType == FillContentType.MIXED) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "导入系统相册",
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (uiState.importToMediaGallery) "直接同步至系统相册索引" else "存放在应用专属隔离目录",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.65f)
                        )
                    }
                    Switch(
                        checked = uiState.importToMediaGallery,
                        onCheckedChange = onMediaGalleryToggle,
                        enabled = !isRunning,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF040D1A),
                            checkedTrackColor = NeonCyan,
                            uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                            uncheckedTrackColor = DarkSurface
                        )
                    )
                }
            }
        }
    }
}
