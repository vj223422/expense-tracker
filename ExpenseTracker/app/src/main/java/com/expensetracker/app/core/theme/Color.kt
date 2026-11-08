package com.expensetracker.app.core.theme

import androidx.compose.ui.graphics.Color

// Premium fintech palette: warm, restrained surfaces with a confident emerald accent.
val primaryLight = Color(0xFF087F5B)
val onPrimaryLight = Color(0xFFFFFFFF)
val primaryContainerLight = Color(0xFFD5F5E8)
val onPrimaryContainerLight = Color(0xFF002116)
val secondaryLight = Color(0xFF52665D)
val onSecondaryLight = Color(0xFFFFFFFF)
val secondaryContainerLight = Color(0xFFD5E8DE)
val onSecondaryContainerLight = Color(0xFF102019)
val tertiaryLight = Color(0xFF496477)
val onTertiaryLight = Color(0xFFFFFFFF)
val tertiaryContainerLight = Color(0xFFD0E8F7)
val onTertiaryContainerLight = Color(0xFF061E2C)
val errorLight = Color(0xFFBA4545)
val onErrorLight = Color(0xFFFFFFFF)
val errorContainerLight = Color(0xFFFFDAD6)
val onErrorContainerLight = Color(0xFF410006)
val backgroundLight = Color(0xFFF5F7F5)
val onBackgroundLight = Color(0xFF18211D)
val surfaceLight = Color(0xFFF5F7F5)
val onSurfaceLight = Color(0xFF18211D)
val surfaceVariantLight = Color(0xFFE2E9E4)
val onSurfaceVariantLight = Color(0xFF46534B)
val outlineLight = Color(0xFF77847B)
val outlineVariantLight = Color(0xFFD0DAD2)
val surfaceContainerLowestLight = Color(0xFFFFFFFF)
val surfaceContainerLowLight = Color(0xFFF0F4F0)
val surfaceContainerLight = Color(0xFFE9EFEB)
val surfaceContainerHighLight = Color(0xFFE2EAE4)
val surfaceContainerHighestLight = Color(0xFFDCE5DE)

val primaryDark = Color(0xFF7CE0B6)
val onPrimaryDark = Color(0xFF003826)
val primaryContainerDark = Color(0xFF00513A)
val onPrimaryContainerDark = Color(0xFF9CF5CE)
val secondaryDark = Color(0xFFB8CCBF)
val onSecondaryDark = Color(0xFF24372D)
val secondaryContainerDark = Color(0xFF3A4D42)
val onSecondaryContainerDark = Color(0xFFD4E8DA)
val tertiaryDark = Color(0xFFB4CCDD)
val onTertiaryDark = Color(0xFF1D3442)
val tertiaryContainerDark = Color(0xFF344B59)
val onTertiaryContainerDark = Color(0xFFD0E8F7)
val errorDark = Color(0xFFFFB4AB)
val onErrorDark = Color(0xFF690006)
val errorContainerDark = Color(0xFF93000D)
val onErrorContainerDark = Color(0xFFFFDAD6)
val backgroundDark = Color(0xFF0D1511)
val onBackgroundDark = Color(0xFFE2ECE5)
val surfaceDark = Color(0xFF0D1511)
val onSurfaceDark = Color(0xFFE2ECE5)
val surfaceVariantDark = Color(0xFF39483F)
val onSurfaceVariantDark = Color(0xFFBACBC0)
val outlineDark = Color(0xFF84968B)
val outlineVariantDark = Color(0xFF39483F)
val surfaceContainerLowestDark = Color(0xFF080E0B)
val surfaceContainerLowDark = Color(0xFF121C16)
val surfaceContainerDark = Color(0xFF17231C)
val surfaceContainerHighDark = Color(0xFF202D25)
val surfaceContainerHighestDark = Color(0xFF2B3930)

// Semantic status colors.
val safeLight = Color(0xFF087F5B)
val warningLight = Color(0xFF946200)
val dangerLight = Color(0xFFBA4545)
val safeDark = Color(0xFF7CE0B6)
val warningDark = Color(0xFFFFC66D)
val dangerDark = Color(0xFFFFB4AB)

// Category colors stay distinct while harmonizing with the app's neutral surfaces.
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
    food = Color(0xFFC46B3D),
    transport = Color(0xFF47758A),
    shopping = Color(0xFF8B65A8),
    bills = Color(0xFF527A69),
    entertainment = Color(0xFFB75C78),
    health = Color(0xFF4E7DA8),
    education = Color(0xFF6D72B5),
    other = Color(0xFF77766B),
)

val categoryPaletteDark = CategoryPalette(
    food = Color(0xFFF0A77D),
    transport = Color(0xFF9BCBDF),
    shopping = Color(0xFFC6A6E3),
    bills = Color(0xFFA8D2BC),
    entertainment = Color(0xFFE5A2B8),
    health = Color(0xFFA5C9ED),
    education = Color(0xFFB9BDF0),
    other = Color(0xFFD0D0C2),
)
