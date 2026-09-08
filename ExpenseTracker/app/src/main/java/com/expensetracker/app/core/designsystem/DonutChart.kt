package com.expensetracker.app.core.designsystem

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.expensetracker.app.core.theme.LocalReducedMotion
import com.expensetracker.app.core.theme.MotionDurations
import com.expensetracker.app.core.theme.MotionEasing

data class DonutSegment(val value: Float, val color: Color, val label: String)

/**
 * A ring chart with rounded, gapped segments that sweep in from 12 o'clock. Reanimates whenever
 * [segments] changes (e.g. a new expense lands in a category) rather than only on first composition.
 */
@Composable
fun CategoryDonutChart(
    segments: List<DonutSegment>,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 22.dp,
    centerContent: @Composable BoxScope.() -> Unit = {},
) {
    val reduceMotion = LocalReducedMotion.current
    val animatedProgress = remember { Animatable(if (reduceMotion) 1f else 0f) }
    LaunchedEffect(segments, reduceMotion) {
        if (reduceMotion) {
            animatedProgress.snapTo(1f)
        } else {
            animatedProgress.snapTo(0f)
            animatedProgress.animateTo(1f, tween(MotionDurations.LONG, easing = MotionEasing.EmphasizedDecelerate))
        }
    }

    val total = segments.sumOf { it.value.toDouble() }.toFloat()
    val strokeWidthPx = with(LocalDensity.current) { strokeWidth.toPx() }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (total <= 0f) return@Canvas
            val gapDegrees = if (segments.size > 1) 4f else 0f
            var startAngle = -90f
            for (segment in segments) {
                val sweep = (segment.value / total) * 360f * animatedProgress.value
                // Cap the gap at half the segment's own sweep so a category with a tiny share of
                // spend still draws a visible sliver instead of the fixed gap swallowing it whole.
                val drawnSweep = (sweep - minOf(gapDegrees, sweep / 2f)).coerceAtLeast(0f)
                if (drawnSweep > 0f) {
                    drawArc(
                        color = segment.color,
                        startAngle = startAngle,
                        sweepAngle = drawnSweep,
                        useCenter = false,
                        style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round),
                    )
                }
                startAngle += sweep
            }
        }
        centerContent()
    }
}
