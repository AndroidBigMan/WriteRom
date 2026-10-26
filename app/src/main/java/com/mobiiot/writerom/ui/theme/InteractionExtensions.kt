package com.mobiiot.writerom.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 物理弹簧按压微动效 Modifier：
 * 按压时产生 0.95f 的阻尼缩放，并在释放时触发轻微触感振动反馈。
 */
fun Modifier.bounceClickable(
    scaleDown: Float = 0.95f,
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) scaleDown else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "bounceScale"
    )

    this
        .scale(scale)
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClick = {
                try {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                } catch (ignored: Exception) {
                }
                onClick()
            }
        )
}

/**
 * 微光环境光渐变描边：
 * 模拟工业级精密仪器的 1dp 边缘光感，上方高光，下方暗隐。
 */
fun Modifier.glassBorder(
    cornerRadius: Dp = 20.dp,
    strokeWidth: Dp = 1.dp
): Modifier = composed {
    val isDark = isSystemInDarkTheme()
    val borderBrush = Brush.verticalGradient(
        colors = if (isDark) {
            listOf(
                Color.White.copy(alpha = 0.16f),
                Color.White.copy(alpha = 0.04f)
            )
        } else {
            listOf(
                Color.Black.copy(alpha = 0.10f),
                Color.Black.copy(alpha = 0.03f)
            )
        }
    )
    this.border(
        width = strokeWidth,
        brush = borderBrush,
        shape = RoundedCornerShape(cornerRadius)
    )
}
