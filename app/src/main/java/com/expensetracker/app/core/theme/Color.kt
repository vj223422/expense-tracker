package com.expensetracker.app.core.theme

import androidx.compose.ui.graphics.Color

// Seed: a calm teal-green — money/growth association without being a cliché finance-app blue.
val primaryLight = Color(0xFF1B6E5C)
val onPrimaryLight = Color(0xFFFFFFFF)
val primaryContainerLight = Color(0xFFA6F2DA)
val onPrimaryContainerLight = Color(0xFF002015)
val secondaryLight = Color(0xFF4A635B)
val onSecondaryLight = Color(0xFFFFFFFF)
val secondaryContainerLight = Color(0xFFCCE8DC)
val onSecondaryContainerLight = Color(0xFF06201A)
val tertiaryLight = Color(0xFF3F6375)
val onTertiaryLight = Color(0xFFFFFFFF)
val tertiaryContainerLight = Color(0xFFC3E8FC)
val onTertiaryContainerLight = Color(0xFF001F2A)
val errorLight = Color(0xFFBA1A1A)
val onErrorLight = Color(0xFFFFFFFF)
val errorContainerLight = Color(0xFFFFDAD6)
val onErrorContainerLight = Color(0xFF410002)
val backgroundLight = Color(0xFFF6FBF7)
val onBackgroundLight = Color(0xFF171D1A)
val surfaceLight = Color(0xFFF6FBF7)
val onSurfaceLight = Color(0xFF171D1A)
val surfaceVariantLight = Color(0xFFDBE5DF)
val onSurfaceVariantLight = Color(0xFF404944)
val outlineLight = Color(0xFF707974)
val outlineVariantLight = Color(0xFFBFC9C3)
val surfaceContainerLowestLight = Color(0xFFFFFFFF)
val surfaceContainerLowLight = Color(0xFFF0F5F1)
val surfaceContainerLight = Color(0xFFEAF0EB)
val surfaceContainerHighLight = Color(0xFFE4EAE5)
val surfaceContainerHighestLight = Color(0xFFDEE4E0)

val primaryDark = Color(0xFF8AD5BE)
val onPrimaryDark = Color(0xFF00382C)
val primaryContainerDark = Color(0xFF005142)
val onPrimaryContainerDark = Color(0xFFA6F2DA)
val secondaryDark = Color(0xFFB0CCC1)
val onSecondaryDark = Color(0xFF1C352E)
val secondaryContainerDark = Color(0xFF334B44)
val onSecondaryContainerDark = Color(0xFFCCE8DC)
val tertiaryDark = Color(0xFFA7CCE0)
val onTertiaryDark = Color(0xFF0A3444)
val tertiaryContainerDark = Color(0xFF264B5C)
val onTertiaryContainerDark = Color(0xFFC3E8FC)
val errorDark = Color(0xFFFFB4AB)
val onErrorDark = Color(0xFF690005)
val errorContainerDark = Color(0xFF93000A)
val onErrorContainerDark = Color(0xFFFFDAD6)
val backgroundDark = Color(0xFF0F1512)
val onBackgroundDark = Color(0xFFDEE4E0)
val surfaceDark = Color(0xFF0F1512)
val onSurfaceDark = Color(0xFFDEE4E0)
val surfaceVariantDark = Color(0xFF404944)
val onSurfaceVariantDark = Color(0xFFBFC9C3)
val outlineDark = Color(0xFF899390)
val outlineVariantDark = Color(0xFF404944)
val surfaceContainerLowestDark = Color(0xFF0A0F0D)
val surfaceContainerLowDark = Color(0xFF171D1A)
val surfaceContainerDark = Color(0xFF1B211E)
val surfaceContainerHighDark = Color(0xFF252B28)
val surfaceContainerHighestDark = Color(0xFF303633)

// Semantic status — budget progress bands. Not part of MaterialTheme.colorScheme's fixed
// roles, so exposed via LocalExtendedColors (see Theme.kt) rather than hardcoded at call sites.
val safeLight = Color(0xFF1B6E5C)
val warningLight = Color(0xFF8A5800)
val dangerLight = Color(0xFFBA1A1A)
val safeDark = Color(0xFF8AD5BE)
val warningDark = Color(0xFFFFB74D)
val dangerDark = Color(0xFFFFB4AB)

// One accent per ExpenseCategory (data/model/ExpenseCategory), tuned as a light/dark pair.
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
    health = Color(0xFF1B6E5C),
    education = Color(0xFF3A5FBF),
    other = Color(0xFF6B6558),
)

val categoryPaletteDark = CategoryPalette(
    food = Color(0xFFF0A876),
    transport = Color(0xFFA7CCE0),
    shopping = Color(0xFFE0AEEC),
    bills = Color(0xFFB0CCC1),
    entertainment = Color(0xFFF0A8C4),
    health = Color(0xFF8AD5BE),
    education = Color(0xFFAEBFF6),
    other = Color(0xFFD6D0C1),
)
