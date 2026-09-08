package com.expensetracker.app.core.designsystem

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.expensetracker.app.core.theme.LocalReducedMotion
import com.expensetracker.app.core.theme.MotionDurations
import com.expensetracker.app.core.theme.MotionEasing
import com.expensetracker.app.core.util.formatAsCurrency

/** Counts smoothly from its previous value to [amountMinor] instead of snapping — a small detail
 * that reads as "alive" wherever a headline total updates (see android-skills:android-ux motion). */
@Composable
fun AnimatedAmountText(
    amountMinor: Long,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = LocalContentColor.current,
) {
    val reduceMotion = LocalReducedMotion.current
    // Long amounts can exceed Float's 24-bit exact-integer range (~₹167,772+), so only a 0..1
    // progress value is animated as a Float; the displayed Long is derived via Double math and
    // pinned to the exact target once the animation settles, instead of animating amountMinor
    // itself as a Float (which would silently round large totals).
    val progress = remember { Animatable(1f) }
    var displayValue by remember { mutableLongStateOf(amountMinor) }

    LaunchedEffect(amountMinor, reduceMotion) {
        // Start from what's actually on screen right now, not the previous target — if this
        // effect replaced one still mid-animation (amountMinor changed again before it finished),
        // starting from the old target instead would make the number visibly jump before
        // continuing on towards the new one.
        val start = displayValue
        val target = amountMinor
        if (reduceMotion || start == target) {
            displayValue = target
            progress.snapTo(1f)
            return@LaunchedEffect
        }
        progress.snapTo(0f)
        progress.animateTo(1f, tween(MotionDurations.MEDIUM, easing = MotionEasing.Standard)) {
            displayValue = (start + (target - start) * value.toDouble()).toLong()
        }
        displayValue = target
    }
    Text(text = displayValue.formatAsCurrency(), modifier = modifier, style = style, color = color)
}
