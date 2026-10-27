package com.mobiiot.writerom.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobiiot.writerom.ui.StorageUiState
import com.mobiiot.writerom.ui.theme.*

/**
 * 方案 C: 现代流光工学派 (Neon Hardware Health Bento)
 * 芯片实时温度、系统热节流监测与闪存损耗 P/E 周期估算
 */
@Composable
fun HardwareHealthCard(
    uiState: StorageUiState,
    modifier: Modifier = Modifier
) {
    val temp = uiState.deviceTemperature
    val (tempColor, tempDesc) = when {
        temp <= 0f -> Pair(MaterialTheme.colorScheme.onSurfaceVariant, "温度读取中")
        temp < 37.0f -> Pair(NeonEmerald, "正常 (冷感运行)")
        temp < 42.0f -> Pair(NeonAmber, "温升 (轻度负载)")
        temp < 46.0f -> Pair(Color(0xFFF97316), "偏热 (高负载工作)")
        else -> Pair(NeonRose, "高温 (触发主控降频)")
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .glassBorder(24.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp)
        ) {
            // 标题栏与 UFS/eMMC 状态胶囊（上下平行排列）
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
                                    listOf(NeonEmerald.copy(alpha = 0.22f), NeonCyan.copy(alpha = 0.10f))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Hardware Health",
                            tint = NeonEmerald,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "闪存健康与芯片感知",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            letterSpacing = (-0.02).sp
                        )
                        Text(
                            text = "热节流保护与 TBW 损耗周期",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = NeonEmerald.copy(alpha = 0.12f),
                    modifier = Modifier
                        .glassBorder(12.dp, 0.6.dp)
                        .bounceClickable(onClick = {})
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(NeonEmerald)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "UFS / eMMC 状态巡检中",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonEmerald
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 温度与热状态 Bento 双格
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 左格：实时核心温度
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.40f),
                    modifier = Modifier
                        .weight(1f)
                        .glassBorder(18.dp, 0.8.dp)
                        .bounceClickable(onClick = {})
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "设备芯片温度",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = if (temp > 0f) String.format("%.1f", temp) else "--.-",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = tempColor
                            )
                            Text(
                                text = " °C",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = tempColor,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = tempDesc,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = tempColor
                        )
                    }
                }

                // 右格：系统热节流状态
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.40f),
                    modifier = Modifier
                        .weight(1f)
                        .glassBorder(18.dp, 0.8.dp)
                        .bounceClickable(onClick = {})
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "热节流 (Throttling)",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = uiState.thermalStatusLabel,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (uiState.thermalStatusLevel >= 3) NeonRose else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (uiState.thermalStatusLevel >= 3)
                                "I/O 调度受温控限制"
                            else
                                "闪存总线全速运行",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 闪存写入寿命感知 (TBW 与 P/E 周期估算 Bento 容器)
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.30f),
                modifier = Modifier
                    .fillMaxWidth()
                    .glassBorder(18.dp, 0.8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "闪存损耗与寿命估算 (TBW)",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = NeonPurple.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "P/E Cycle",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonPurple,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "累计总写入量",
                                fontSize = 12.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = uiState.formattedLifetimeBytes,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = NeonCyan
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "等效整盘擦写周期",
                                fontSize = 12.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = String.format("约 %.2f 次循环", uiState.estimatedPeCycles),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = NeonPurple
                            )
                        }
                    }
                }
            }
        }
    }
}
