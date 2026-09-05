package com.expensetracker.app.core.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing

/**
 * M3 duration ladder (see android-skills:android-ux) — one source of truth so screens never
 * hand-roll arbitrary millis.
 */
object MotionDurations {
    const val SHORT = 150
    const val MEDIUM = 300
    const val LONG = 500
    const val EXTRA_LONG = 800
}

object MotionEasing {
    val Standard: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val Emphasized: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val EmphasizedAccelerate: Easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
    val EmphasizedDecelerate: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
}
