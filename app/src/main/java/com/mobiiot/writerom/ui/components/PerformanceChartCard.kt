package com.mobiiot.writerom.ui.components

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobiiot.writerom.model.TaskStatus
import com.mobiiot.writerom.ui.StorageUiState
import com.mobiiot.writerom.ui.theme.bounceClickable
import com.mobiiot.writerom.ui.theme.glassBorder
import kotlin.math.max

@Composable
fun PerformanceChartCard(
    uiState: StorageUiState,
    modifier: Modifier = Modifier
) {
    val history = uiState.speedHistory
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

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
            // 标题行
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(primaryColor.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Performance Chart",
                            tint = primaryColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "实时写入性能衰减曲线",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "SLC 缓存与直写颗粒特征遥测",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (uiState.isSlcExhausted)
                        tertiaryColor.copy(alpha = 0.15f)
                    else
                        primaryColor.copy(alpha = 0.12f),
                    modifier = Modifier.bounceClickable(onClick = {})
                ) {
                    Text(
                        text = if (uiState.isSlcExhausted) "⚡ SLC Cache 已耗尽" else "SLC 缓存加速中",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (uiState.isSlcExhausted) tertiaryColor else primaryColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 指标快速汇总 Bento 模块
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricItem("当前速率", uiState.formattedSpeed, modifier = Modifier.weight(1f))
                MetricItem("峰值速率", String.format("%.1f MB/s", uiState.peakSpeedMb), modifier = Modifier.weight(1f))
                MetricItem("全程均速", String.format("%.1f MB/s", uiState.avgSpeedMb), modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Canvas 折线图绘制区域
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .background(surfaceVariant.copy(alpha = 0.25f), shape = RoundedCornerShape(12.dp))
                    .glassBorder(12.dp)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                if (history.size < 2) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (uiState.taskStatus == TaskStatus.IDLE)
                                "启动填充后将实时采样并绘制闪存性能衰减趋势"
                            else
                                "正在采集初始写入样本 (每秒刷新)...",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val width = size.width
                        val height = size.height

                        val peak = max(uiState.peakSpeedMb, 20f)
                        val maxY = peak * 1.15f // 预留 15% 顶部空间

                        // 绘制 3 条参考虚线 (25%, 50%, 75%)
                        val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                        listOf(0.25f, 0.5f, 0.75f).forEach { fraction ->
                            val y = height * (1f - fraction)
                            drawLine(
                                color = surfaceVariant.copy(alpha = 0.8f),
                                start = Offset(0f, y),
                                end = Offset(width, y),
                                strokeWidth = 1f,
                                pathEffect = dashEffect
                            )
                        }

                        // 计算折线与贝塞尔控制点
                        val points = mutableListOf<Offset>()
                        val stepX = width / (history.size - 1)
                        for (i in history.indices) {
                            val x = i * stepX
                            val speed = history[i]
                            val y = height - (speed / maxY) * height
                            points.add(Offset(x, y.coerceIn(0f, height)))
                        }

                        // 构建平滑贝塞尔曲线路径
                        val strokePath = Path().apply {
                            moveTo(points[0].x, points[0].y)
                            for (i in 1 until points.size) {
                                val prev = points[i - 1]
                                val curr = points[i]
                                val cx1 = (prev.x + curr.x) / 2f
                                val cy1 = prev.y
                                val cx2 = (prev.x + curr.x) / 2f
                                val cy2 = curr.y
                                cubicTo(cx1, cy1, cx2, cy2, curr.x, curr.y)
                            }
                        }

                        // 构建渐变填充闭合路径
                        val fillPath = Path().apply {
                            addPath(strokePath)
                            lineTo(points.last().x, height)
                            lineTo(points.first().x, height)
                            close()
                        }

                        // 绘制下方微光渐变
                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    primaryColor.copy(alpha = 0.35f),
                                    primaryColor.copy(alpha = 0.03f),
                                    Color.Transparent
                                ),
                                startY = 0f,
                                endY = height
                            )
                        )

                        // 绘制平均线 (粉虚线)
                        if (uiState.avgSpeedMb > 0) {
                            val yAvg = (height - (uiState.avgSpeedMb / maxY) * height).coerceIn(0f, height)
                            drawLine(
                                color = tertiaryColor.copy(alpha = 0.6f),
                                start = Offset(0f, yAvg),
                                end = Offset(width, yAvg),
                                strokeWidth = 2f,
                                pathEffect = dashEffect
                            )
                        }

                        // 绘制主折线
                        drawPath(
                            path = strokePath,
                            color = primaryColor,
                            style = Stroke(
                                width = 2.5.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        )

                        // 绘制末端当前点高亮
                        val lastPoint = points.last()
                        drawCircle(
                            color = primaryColor.copy(alpha = 0.3f),
                            radius = 6.dp.toPx(),
                            center = lastPoint
                        )
                        drawCircle(
                            color = primaryColor,
                            radius = 3.dp.toPx(),
                            center = lastPoint
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 说明注解
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (uiState.isSlcExhausted)
                        "⚠️ 写入速率大幅下降：模拟真实闪存 SLC Cache 耗尽后的物理颗粒直写状态"
                    else
                        "💡 闪存特征：初期由 SLC Cache 高速爆发，耗尽后将回落至物理直写区",
                    fontSize = 11.sp,
                    color = if (uiState.isSlcExhausted) tertiaryColor else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MetricItem(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        modifier = modifier
            .glassBorder(12.dp)
            .bounceClickable(onClick = {})
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
