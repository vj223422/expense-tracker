package com.expensetracker.app.core.designsystem

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
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
    val animatedValue = remember { Animatable(amountMinor.toFloat()) }
    LaunchedEffect(amountMinor, reduceMotion) {
        if (reduceMotion) animatedValue.snapTo(amountMinor.toFloat())
        else animatedValue.animateTo(amountMinor.toFloat(), tween(MotionDurations.MEDIUM, easing = MotionEasing.Standard))
    }
    Text(text = animatedValue.value.toLong().formatAsCurrency(), modifier = modifier, style = style, color = color)
}
