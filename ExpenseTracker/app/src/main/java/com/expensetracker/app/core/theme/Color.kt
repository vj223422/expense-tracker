package com.expensetracker.app.core.theme

import androidx.compose.ui.graphics.Color

// App-wide blue theme. All Material surfaces, controls and primary actions derive from this palette.
val primaryLight = Color(0xFF1565D8)
val onPrimaryLight = Color(0xFFFFFFFF)
val primaryContainerLight = Color(0xFFD9E7FF)
val onPrimaryContainerLight = Color(0xFF001B3D)
val secondaryLight = Color(0xFF536273)
val onSecondaryLight = Color(0xFFFFFFFF)
val secondaryContainerLight = Color(0xFFD7E3F4)
val onSecondaryContainerLight = Color(0xFF101D2A)
val tertiaryLight = Color(0xFF5B5F8A)
val onTertiaryLight = Color(0xFFFFFFFF)
val tertiaryContainerLight = Color(0xFFE2E0FF)
val onTertiaryContainerLight = Color(0xFF171633)
val errorLight = Color(0xFFBA1A1A)
val onErrorLight = Color(0xFFFFFFFF)
val errorContainerLight = Color(0xFFFFDAD6)
val onErrorContainerLight = Color(0xFF410002)
val backgroundLight = Color(0xFFF7F9FC)
val onBackgroundLight = Color(0xFF171B22)
val surfaceLight = Color(0xFFF7F9FC)
val onSurfaceLight = Color(0xFF171B22)
val surfaceVariantLight = Color(0xFFDCE3EC)
val onSurfaceVariantLight = Color(0xFF414952)
val outlineLight = Color(0xFF717982)
val outlineVariantLight = Color(0xFFC0C8D2)
val surfaceContainerLowestLight = Color(0xFFFFFFFF)
val surfaceContainerLowLight = Color(0xFFF0F3F8)
val surfaceContainerLight = Color(0xFFEAEFF5)
val surfaceContainerHighLight = Color(0xFFE4E9F0)
val surfaceContainerHighestLight = Color(0xFFDEE4EB)

val primaryDark = Color(0xFF8FB8FF)
val onPrimaryDark = Color(0xFF002F6C)
val primaryContainerDark = Color(0xFF084A9B)
val onPrimaryContainerDark = Color(0xFFD9E7FF)
val secondaryDark = Color(0xFFBBC8DA)
val onSecondaryDark = Color(0xFF25313E)
val secondaryContainerDark = Color(0xFF3B4857)
val onSecondaryContainerDark = Color(0xFFD7E3F4)
val tertiaryDark = Color(0xFFC4C3F0)
val onTertiaryDark = Color(0xFF2D2D50)
val tertiaryContainerDark = Color(0xFF444466)
val onTertiaryContainerDark = Color(0xFFE2E0FF)
val errorDark = Color(0xFFFFB4AB)
val onErrorDark = Color(0xFF690005)
val errorContainerDark = Color(0xFF93000A)
val onErrorContainerDark = Color(0xFFFFDAD6)
val backgroundDark = Color(0xFF090E17)
val onBackgroundDark = Color(0xFFE1E7F0)
val surfaceDark = Color(0xFF090E17)
val onSurfaceDark = Color(0xFFE1E7F0)
val surfaceVariantDark = Color(0xFF3F4854)
val onSurfaceVariantDark = Color(0xFFBEC7D2)
val outlineDark = Color(0xFF89929E)
val outlineVariantDark = Color(0xFF3F4854)
val surfaceContainerLowestDark = Color(0xFF060A10)
val surfaceContainerLowDark = Color(0xFF111720)
val surfaceContainerDark = Color(0xFF161D27)
val surfaceContainerHighDark = Color(0xFF202833)
val surfaceContainerHighestDark = Color(0xFF2A3440)

// Semantic status colors remain distinct from the blue application accent.
val safeLight = Color(0xFF1B6E5C)
val warningLight = Color(0xFF8A5800)
val dangerLight = Color(0xFFBA1A1A)
val safeDark = Color(0xFF8AD5BE)
val warningDark = Color(0xFFFFB74D)
val dangerDark = Color(0xFFFFB4AB)

// Category colors remain differentiated so expense categories are easy to scan.
data class CategoryPalette(
    val food: Color,
    val transport: Color,
    val shopping: Color,
    val bills: Color,
    val entertainment: Color,
    val health: Color,
    val education: Color,
    val other: Color,
)

val categoryPaletteLight = CategoryPalette(
    food = Color(0xFFC96A2E),
    transport = Color(0xFF3F6375),
    shopping = Color(0xFF8B4A9C),
    bills = Color(0xFF4A635B),
    entertainment = Color(0xFFB2416B),
    health = Color(0xFF1565D8),
    education = Color(0xFF3A5FBF),
    other = Color(0xFF6B6558),
)

val categoryPaletteDark = CategoryPalette(
    food = Color(0xFFF0A876),
    transport = Color(0xFFA7CCE0),
    shopping = Color(0xFFE0AEEC),
    bills = Color(0xFFB0CCC1),
    entertainment = Color(0xFFF0A8C4),
    health = Color(0xFF8FB8FF),
    education = Color(0xFFAEBFF6),
    other = Color(0xFFD6D0C1),
)
