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
import com.mobiiot.writerom.ui.theme.*
import kotlin.math.max

/**
 * 方案 C: 现代流光工学派 (Neon Performance Chart)
 * 具备平滑贝塞尔曲线、下渗光感微光填充与 SLC 耗尽态自适应霓虹变色
 */
@Composable
fun PerformanceChartCard(
    uiState: StorageUiState,
    modifier: Modifier = Modifier
) {
    val history = uiState.speedHistory
    val curveColor = if (uiState.isSlcExhausted) NeonAmber else NeonCyan
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

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
            // 标题行与 SLC 状态胶囊（上下平行排列）
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
                                    listOf(NeonCyan.copy(alpha = 0.20f), NeonPurple.copy(alpha = 0.12f))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Performance Chart",
                            tint = NeonCyan,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "实时写入性能衰减曲线",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            letterSpacing = (-0.02).sp
                        )
                        Text(
                            text = "SLC Cache 模拟缓存与原生颗粒感知",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (uiState.isSlcExhausted) NeonAmber.copy(alpha = 0.15f) else NeonCyan.copy(alpha = 0.12f),
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
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (uiState.isSlcExhausted) NeonAmber else NeonCyan)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (uiState.isSlcExhausted) "⚡ SLC Cache 已耗尽 (原生直写)" else "⚡ SLC 缓存加速中 (高吞吐)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (uiState.isSlcExhausted) NeonAmber else NeonCyan
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 指标快速汇总 Bento 模块 (流光多格)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricItem("当前瞬时速率", uiState.formattedSpeed, NeonCyan, modifier = Modifier.weight(1f))
                MetricItem("峰值写入速率", String.format("%.1f MB/s", uiState.peakSpeedMb), ElectricBlue, modifier = Modifier.weight(1f))
                MetricItem("全任务均速", String.format("%.1f MB/s", uiState.avgSpeedMb), NeonPurple, modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Canvas 折线图绘制区域 (流光液态图版)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(surfaceVariant.copy(alpha = 0.35f), shape = RoundedCornerShape(18.dp))
                    .glassBorder(18.dp, 0.6.dp)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                if (history.size < 2) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (uiState.taskStatus == TaskStatus.IDLE)
                                "启动填充后将实时采样并绘制闪存性能衰减趋势"
                            else
                                "正在采集初始写入样本 (每秒高频采样)...",
                            fontSize = 13.5.sp,
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
                                color = surfaceVariant.copy(alpha = 0.6f),
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

                        // 绘制下方微光流光渐变
                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    curveColor.copy(alpha = 0.35f),
                                    curveColor.copy(alpha = 0.05f),
                                    Color.Transparent
                                ),
                                startY = 0f,
                                endY = height
                            )
                        )

                        // 绘制全程均速参考线
                        if (uiState.avgSpeedMb > 0) {
                            val yAvg = (height - (uiState.avgSpeedMb / maxY) * height).coerceIn(0f, height)
                            drawLine(
                                color = NeonPurple.copy(alpha = 0.65f),
                                start = Offset(0f, yAvg),
                                end = Offset(width, yAvg),
                                strokeWidth = 1.5.dp.toPx(),
                                pathEffect = dashEffect
                            )
                        }

                        // 绘制主曲线 (带霓虹高光)
                        drawPath(
                            path = strokePath,
                            color = curveColor,
                            style = Stroke(
                                width = 3.5.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        )

                        // 绘制末端当前最新采样点高光光环
                        val lastPoint = points.last()
                        drawCircle(
                            color = curveColor.copy(alpha = 0.25f),
                            radius = 9.dp.toPx(),
                            center = lastPoint
                        )
                        drawCircle(
                            color = curveColor,
                            radius = 4.dp.toPx(),
                            center = lastPoint
                        )
                    }
                }
            }

            if (uiState.isSlcExhausted) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = NeonAmber.copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⚠️ SLC 缓存已耗尽，已回落至闪存颗粒原生直写区 (GC/搬迁负载上升)",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = NeonAmber
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricItem(label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.40f),
        modifier = modifier
            .glassBorder(16.dp, 0.8.dp)
            .bounceClickable(onClick = {})
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = value,
                fontSize = 15.5.sp,
                fontWeight = FontWeight.ExtraBold,
                color = accent
            )
        }
    }
}
