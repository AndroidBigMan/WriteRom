package com.mobiiot.writerom.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
 * 方案 C: 现代流光工学派 (Neon Glassmorphic Hero Bento)
 * 具备多段能量霓虹流光刻度规与毛玻璃 Bento 层级
 */
@Composable
fun StorageOverviewCard(
    uiState: StorageUiState,
    modifier: Modifier = Modifier
) {
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
            // 标题栏与物理容量胶囊（上下平行排列）
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
                                    listOf(NeonCyan.copy(alpha = 0.22f), ElectricBlue.copy(alpha = 0.10f))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Storage",
                            tint = NeonCyan,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "手机 ROM 存储状态",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            letterSpacing = (-0.02).sp
                        )
                        Text(
                            text = "底层物理介质空间实时感知",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = NeonCyan.copy(alpha = 0.12f),
                    modifier = Modifier
                        .glassBorder(12.dp, 0.5.dp)
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
                                .background(NeonCyan)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "总物理容量: ${uiState.formattedTotal}",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 存储进度条与占比计算
            val progressFraction = (uiState.percentUsed / 100.0).toFloat().coerceIn(0f, 1f)
            val animatedProgress by animateFloatAsState(
                targetValue = progressFraction,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "storageProgress"
            )

            // 现代流光工学：多段能量光谱 (Neon Spectral Gradient)
            val gaugeGradientColors = when {
                uiState.percentUsed < 80.0 -> listOf(
                    NeonEmerald,
                    NeonCyan,
                    ElectricBlue
                )
                uiState.percentUsed < 92.0 -> listOf(
                    NeonCyan,
                    NeonAmber,
                    Color(0xFFF97316)
                )
                else -> listOf(
                    NeonAmber,
                    NeonRose,
                    Color(0xFFFF0055)
                )
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(gaugeGradientColors.last())
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "已占用: ${uiState.formattedUsed}",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "${String.format("%.1f", uiState.percentUsed)}%",
                        fontSize = 18.sp,
                        color = gaugeGradientColors.last(),
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 现代霓虹流光能量进度条 (带高光折射与呼吸圆角)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(26.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f))
                        .glassBorder(cornerRadius = 13.dp, strokeWidth = 0.5.dp)
                ) {
                    // 渐变能量填充层
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedProgress)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(13.dp))
                            .background(Brush.horizontalGradient(gaugeGradientColors))
                    ) {
                        // 顶部液态高光微光反射 (Liquid Glass Top Specular)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(9.dp)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color.White.copy(alpha = 0.38f),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 底部 Bento 形式细项标签卡 (带双格微光描边)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BentoLegendPill(
                    indicatorColor = gaugeGradientColors.last(),
                    label = "已使用空间",
                    value = uiState.formattedUsed,
                    modifier = Modifier.weight(1f)
                )
                BentoLegendPill(
                    indicatorColor = NeonEmerald,
                    label = "当前可填充",
                    value = uiState.formattedFree,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun BentoLegendPill(
    indicatorColor: Color,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.40f),
        modifier = modifier
            .glassBorder(16.dp, 0.8.dp)
            .bounceClickable(onClick = {})
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(indicatorColor)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = label,
                    fontSize = 12.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = value,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
