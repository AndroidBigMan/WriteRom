package com.mobiiot.writerom.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobiiot.writerom.ui.StorageUiState
import com.mobiiot.writerom.ui.theme.bounceClickable
import com.mobiiot.writerom.ui.theme.glassBorder

@Composable
fun HardwareHealthCard(
    uiState: StorageUiState,
    modifier: Modifier = Modifier
) {
    val temp = uiState.deviceTemperature
    val (tempColor, tempDesc) = when {
        temp <= 0f -> Pair(MaterialTheme.colorScheme.onSurfaceVariant, "温度读取中")
        temp < 37.0f -> Pair(Color(0xFF10B981), "正常 (冷感)")
        temp < 42.0f -> Pair(Color(0xFFF59E0B), "温升 (轻度负载)")
        temp < 46.0f -> Pair(Color(0xFFF97316), "偏热 (高负载)")
        else -> Pair(Color(0xFFEF4444), "高温 (触发主控保护)")
    }

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
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Hardware Health",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "闪存健康与芯片感知",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    modifier = Modifier.bounceClickable(onClick = {})
                ) {
                    Text(
                        text = "UFS / eMMC 状态",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 温度与热状态 Bento 双格 (放大内部字号与尺寸)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 左格：实时核心温度
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier
                        .weight(1f)
                        .glassBorder(16.dp)
                        .bounceClickable(onClick = {})
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "设备芯片温度",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = if (temp > 0f) String.format("%.1f", temp) else "--.-",
                                fontSize = 30.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = tempColor
                            )
                            Text(
                                text = " °C",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = tempColor,
                                modifier = Modifier.padding(bottom = 3.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = tempDesc,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = tempColor
                        )
                    }
                }

                // 右格：系统热节流状态
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier
                        .weight(1f)
                        .glassBorder(16.dp)
                        .bounceClickable(onClick = {})
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "系统热降频 (Throttling)",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = uiState.thermalStatusLabel,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (uiState.thermalStatusLevel >= 3) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (uiState.thermalStatusLevel >= 3)
                                "I/O 调度受温控限制"
                            else
                                "闪存总线全速运行",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 闪存写入寿命感知 (TBW 与 P/E 周期估算 Bento 容器)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                modifier = Modifier
                    .fillMaxWidth()
                    .glassBorder(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "闪存损耗与寿命估算 (TBW)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "历史总写入量",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = uiState.formattedLifetimeBytes,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "等效整盘 P/E 擦写周期",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = String.format("约 %.2f 次循环", uiState.estimatedPeCycles),
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}
