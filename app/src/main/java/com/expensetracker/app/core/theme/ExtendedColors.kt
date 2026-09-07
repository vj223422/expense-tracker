package com.expensetracker.app.core.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Theme tokens beyond M3's three color slots (see compose/references/theming-material3.md):
 * budget-status bands and per-category accents. Extended via CompositionLocal, never as a
 * global val, so light/dark and future dynamic-color variants stay consistent.
 */
data class ExtendedColors(
    val safe: Color,
    val warning: Color,
    val danger: Color,
    val category: CategoryPalette,
)

val LocalExtendedColors = staticCompositionLocalOf {
    ExtendedColors(
        safe = safeLight,
        warning = warningLight,
        danger = dangerLight,
        category = categoryPaletteLight,
    )
}

val LocalReducedMotion = staticCompositionLocalOf { false }
